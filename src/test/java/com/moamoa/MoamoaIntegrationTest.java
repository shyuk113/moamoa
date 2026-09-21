package com.moamoa;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.moamoa.domain.contest.*;
import com.moamoa.domain.notification.*;
import com.moamoa.domain.user.*;
import com.moamoa.dto.AuthRequests.*;
import com.moamoa.dto.ContestSearchCondition;
import com.moamoa.repository.contest.*;
import com.moamoa.repository.favorite.*;
import com.moamoa.repository.notification.*;
import com.moamoa.repository.user.*;
import com.moamoa.security.jwt.*;
import com.moamoa.service.favorite.*;
import com.moamoa.service.ingest.*;
import com.moamoa.service.notification.*;
import com.moamoa.service.user.*;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
    properties = {
      "app.scheduling-enabled=false",
      "app.seoul-api-key=",
      "app.jwt-secret=abcdefghijklmnopqrstuvwxyz1234567890"
    })
@AutoConfigureMockMvc
@Testcontainers
class MoamoaIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired ContestRepository contests;
  @Autowired UserRepository users;
  @Autowired InterestRepository interests;
  @Autowired FavoriteRepository favorites;
  @Autowired NotificationRepository logs;
  @Autowired AuthService auth;
  @Autowired UserService userService;
  @Autowired FavoriteService favoriteService;
  @Autowired ContestWriter writer;
  @Autowired NotificationDelivery delivery;
  @Autowired NotificationService notifications;
  @Autowired JwtTokenProvider tokens;
  @Autowired Clock clock;
  @MockitoBean EmailSenderService mail;

  @BeforeEach
  void clean() {
    logs.deleteAll();
    favorites.deleteAll();
    interests.deleteAll();
    contests.deleteAll();
    users.deleteAll();
    reset(mail);
  }

  private User user(String email) {
    return auth.signup(new Signup(email, "Password123!", "테스터"));
  }

  private Contest event(String sourceId, ContestCategory category, int start, int end) {
    var c = new Contest();
    c.setSource(ContestSource.SEOUL_OPENAPI);
    c.setSourceId(sourceId);
    c.setTitle("서울 테스트 " + sourceId);
    c.setCategory(category);
    c.setType(
        category == ContestCategory.ART
            ? ContestType.EXHIBITION
            : category == ContestCategory.FAIR ? ContestType.FAIR : ContestType.FESTIVAL);
    c.setRegion(ContestRegion.SEOUL);
    c.setDistrict("종로구");
    c.setOnOffline(OnOfflineType.OFFLINE);
    c.setStartDate(LocalDate.now(clock).plusDays(start));
    c.setEndDate(LocalDate.now(clock).plusDays(end));
    c.setFee("무료");
    c.setOriginalCategory("전시/미술");
    return contests.saveAndFlush(c);
  }

  @Test
  void browserCsrfSurvivesAuthenticatedPageAndStaticAssets() throws Exception {
    var account = user("browser-csrf@example.test");
    var c = event("browser-csrf", ContestCategory.ART, -1, 5);
    var access = new Cookie("ACCESS_TOKEN", tokens.issue(account));
    var page = mvc.perform(get("/contests").cookie(access)).andExpect(status().isOk()).andReturn();
    var csrfCookie = page.getResponse().getCookie("XSRF-TOKEN");
    assertThat(csrfCookie).isNotNull();
    var matcher =
        java.util.regex.Pattern.compile("name=\"_csrf\" content=\"([^\"]+)\"")
            .matcher(page.getResponse().getContentAsString());
    assertThat(matcher.find()).isTrue();
    var pageToken = matcher.group(1);
    for (String asset : List.of("/css/app.css", "/js/app.js")) {
      var response =
          mvc.perform(get(asset).cookie(access, csrfCookie))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse();
      assertThat(response.getHeaders("Set-Cookie"))
          .noneMatch(value -> value.startsWith("XSRF-TOKEN="));
    }
    mvc.perform(
            post("/api/favorites/" + c.getId())
                .cookie(access, csrfCookie)
                .header("X-XSRF-TOKEN", pageToken))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/favorites/" + c.getId()).cookie(access, csrfCookie))
        .andExpect(status().isForbidden());
    mvc.perform(
            delete("/api/favorites/" + c.getId())
                .cookie(access, csrfCookie)
                .header("X-XSRF-TOKEN", pageToken))
        .andExpect(status().isOk());
  }

  @Test
  void rendersAllPublicPagesAndSearchEmptyState() throws Exception {
    for (String url :
        List.of(
            "/",
            "/contests",
            "/auth/login",
            "/auth/signup",
            "/auth/forgot-password",
            "/auth/reset-password")) mvc.perform(get(url)).andExpect(status().isOk());
    mvc.perform(get("/contests"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("아직 발견한 행사가 없어요")));
    mvc.perform(get("/mypage/interests")).andExpect(status().is3xxRedirection());
    mvc.perform(get("/contests/999")).andExpect(status().isNotFound());
  }

  @Test
  void goldenPathSignupLoginSearchFavoriteAndSettings() throws Exception {
    var c = event("golden", ContestCategory.ART, -2, 10);
    mvc.perform(
            post("/api/auth/signup")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"email\":\"golden@example.test\",\"password\":\"Password123!\",\"nickname\":\"테스터\"}"))
        .andExpect(status().isCreated());
    var result =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .contentType("application/json")
                    .content("{\"email\":\"golden@example.test\",\"password\":\"Password123!\"}"))
            .andExpect(status().isOk())
            .andReturn();
    String cookie = result.getResponse().getHeader("Set-Cookie");
    assertThat(cookie).contains("HttpOnly", "SameSite=Lax");
    Cookie access =
        new Cookie("ACCESS_TOKEN", cookie.split(";")[0].substring("ACCESS_TOKEN=".length()));
    mvc.perform(post("/api/favorites/" + c.getId()).cookie(access).with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(post("/api/favorites/" + c.getId()).cookie(access).with(csrf()))
        .andExpect(status().isOk());
    assertThat(favorites.count()).isEqualTo(1);
    mvc.perform(get("/contests").cookie(access))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("♥ 모아둠")));
    mvc.perform(get("/contests/" + c.getId()).cookie(access))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("행사 종료")));
    mvc.perform(get("/mypage/favorites").cookie(access)).andExpect(status().isOk());
    mvc.perform(
            put("/api/interests")
                .cookie(access)
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"categories\":[\"ART\",\"FAIR\",\"FESTIVAL\"],\"frequency\":\"WEEKLY\"}"))
        .andExpect(status().isOk());
    mvc.perform(get("/mypage/interests").cookie(access)).andExpect(status().isOk());
    mvc.perform(delete("/api/favorites/" + c.getId()).cookie(access).with(csrf()))
        .andExpect(status().isOk());
    assertThat(favorites.count()).isZero();
  }

  @Test
  void lockoutPersistsAndExpiredLockAllowsLogin() {
    var u = user("lock@example.test");
    for (int i = 0; i < 5; i++)
      assertThatThrownBy(() -> auth.login(new Login("lock@example.test", "incorrect")))
          .hasMessageContaining(i == 4 ? "15분" : "이메일");
    assertThat(users.findById(u.getId()).orElseThrow().getLoginFailCount()).isEqualTo(5);
    assertThatThrownBy(() -> auth.login(new Login("lock@example.test", "Password123!")))
        .hasMessageContaining("분 후");
    u = users.findById(u.getId()).orElseThrow();
    u.setLockedUntil(clock.instant().minusSeconds(1));
    users.saveAndFlush(u);
    assertThat(auth.login(new Login("lock@example.test", "Password123!")).getLoginFailCount())
        .isZero();
  }

  @Test
  void resetTokenIsHashedSingleUseAndInvalidatesOldJwt() throws Exception {
    var u = user("reset@example.test");
    String jwt = tokens.issue(u);
    auth.forgot(u.getEmail());
    var capture = ArgumentCaptor.forClass(String.class);
    verify(mail).sendReset(eq(u.getEmail()), capture.capture());
    String raw = capture.getValue().split("token=")[1];
    assertThat(users.findById(u.getId()).orElseThrow().getResetToken()).isNotEqualTo(raw);
    auth.reset(new Reset(raw, "NewPassword123!"));
    assertThatThrownBy(() -> auth.reset(new Reset(raw, "NewPassword234!")))
        .hasMessageContaining("이미 사용");
    assertThat(auth.login(new Login(u.getEmail(), "NewPassword123!"))).isNotNull();
    mvc.perform(get("/mypage/favorites").cookie(new Cookie("ACCESS_TOKEN", jwt)))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void expiredResetAndDuplicateEmailAreRejected() {
    var u = user("expired@example.test");
    assertThatThrownBy(() -> user("expired@example.test")).hasMessageContaining("이미 가입");
    auth.forgot(u.getEmail());
    var capture = ArgumentCaptor.forClass(String.class);
    verify(mail).sendReset(eq(u.getEmail()), capture.capture());
    String raw = capture.getValue().split("token=")[1];
    u = users.findById(u.getId()).orElseThrow();
    u.setResetTokenExpiry(clock.instant().minusSeconds(1));
    users.saveAndFlush(u);
    assertThatThrownBy(() -> auth.reset(new Reset(raw, "NewPassword123!")))
        .hasMessageContaining("만료");
  }

  @Test
  void csrfAndAnonymousProtection() throws Exception {
    mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/favorites/1").with(csrf())).andExpect(status().isUnauthorized());
    mvc.perform(get("/mypage/favorites").cookie(new Cookie("ACCESS_TOKEN", "bad-token")))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void queryUsesOverlappingDatesAndHidesEndedEvents() {
    var included = event("art", ContestCategory.ART, -5, 5);
    event("fair", ContestCategory.FAIR, 0, 10);
    event("ended", ContestCategory.ART, -10, -1);
    var c = new ContestSearchCondition();
    c.setCategory(ContestCategory.ART);
    c.setRegion(ContestRegion.SEOUL);
    c.setDistrict("종로구");
    c.setFrom(LocalDate.now(clock));
    c.setTo(LocalDate.now(clock).plusDays(1));
    c.setOnOffline(OnOfflineType.OFFLINE);
    c.setFreeOnly(true);
    c.setKeyword("테스트");
    var page = contests.search(c, org.springframework.data.domain.PageRequest.of(0, 12));
    assertThat(page.getContent()).extracting(Contest::getId).containsExactly(included.getId());
    assertThat(
            contests.search(c, org.springframework.data.domain.PageRequest.of(1, 12)).getContent())
        .isEmpty();
  }

  @Test
  void upsertUpdatesExistingEventWithoutDuplicates() {
    var c = event("stable", ContestCategory.FAIR, 0, 4);
    var incoming = new Contest();
    org.springframework.beans.BeanUtils.copyProperties(c, incoming);
    incoming.setId(null);
    incoming.setTitle("수정된 박람회");
    assertThat(writer.upsert(incoming)).isEqualTo(ContestWriter.Result.UPDATED);
    assertThat(contests.count()).isEqualTo(1);
    assertThat(contests.findById(c.getId()).orElseThrow().getTitle()).isEqualTo("수정된 박람회");
  }

  @Test
  void immediateAndDeadlineDeliveryAreDeduplicatedAndFailedUserDoesNotBlockOthers() {
    var bad = user("bad@example.test");
    var good = user("good@example.test");
    var c = event("notify", ContestCategory.FESTIVAL, 0, 3);
    userService.update(
        bad.getId(), Set.of(ContestCategory.FESTIVAL), NotificationFrequency.IMMEDIATE);
    userService.update(
        good.getId(), Set.of(ContestCategory.FESTIVAL), NotificationFrequency.IMMEDIATE);
    doThrow(new IllegalStateException("SMTP unavailable"))
        .when(mail)
        .sendEvents(eq(bad.getEmail()), anyString(), anyList());
    notifications.deadlines();
    notifications.deadlines();
    verify(mail, times(1)).sendEvents(eq(good.getEmail()), anyString(), anyList());
    assertThat(
            logs.findByUserIdAndContestIdAndType(bad.getId(), c.getId(), NotificationType.DEADLINE)
                .orElseThrow()
                .isSuccess())
        .isFalse();
    assertThat(logs.count()).isEqualTo(2);
  }

  @Test
  void weeklySendsOneMailAndSkipsZeroPending() {
    var u = user("weekly@example.test");
    userService.update(u.getId(), Set.of(ContestCategory.ART), NotificationFrequency.WEEKLY);
    delivery.digest(u.getId());
    verify(mail, never()).sendEvents(anyString(), anyString(), anyList());
    var a = event("a", ContestCategory.ART, 0, 5);
    var b = event("b", ContestCategory.ART, 0, 8);
    delivery.deliver(u.getId(), a.getId(), NotificationType.NEW_CONTEST);
    delivery.deliver(u.getId(), b.getId(), NotificationType.NEW_CONTEST);
    verify(mail, never()).sendEvents(anyString(), anyString(), anyList());
    notifications.weekly();
    notifications.weekly();
    verify(mail, times(1))
        .sendEvents(eq(u.getEmail()), anyString(), argThat(list -> list.size() == 2));
  }

  @Test
  void earlyEndUpdatesExistingButDoesNotImportOldNewEvents() {
    var c = event("early", ContestCategory.ART, -5, 5);
    var incoming = new Contest();
    org.springframework.beans.BeanUtils.copyProperties(c, incoming);
    incoming.setId(null);
    incoming.setEndDate(LocalDate.now(clock).minusDays(1));
    assertThat(writer.upsert(incoming)).isEqualTo(ContestWriter.Result.UPDATED);
    assertThat(
            contests
                .search(
                    new ContestSearchCondition(),
                    org.springframework.data.domain.PageRequest.of(0, 12))
                .getTotalElements())
        .isZero();
    incoming.setSourceId("unknown-old");
    assertThat(writer.upsert(incoming)).isEqualTo(ContestWriter.Result.SKIPPED);
    assertThat(contests.count()).isEqualTo(1);
  }

  @Test
  void afterCommitNewEventTriggersMatchingRecipientOnly() {
    var u = user("matching@example.test");
    userService.update(u.getId(), Set.of(ContestCategory.ART), NotificationFrequency.IMMEDIATE);
    var c = new Contest();
    c.setTitle("새 전시");
    c.setSource(ContestSource.SEOUL_OPENAPI);
    c.setSourceId("after-commit");
    c.setCategory(ContestCategory.ART);
    c.setType(ContestType.EXHIBITION);
    c.setRegion(ContestRegion.SEOUL);
    c.setOnOffline(OnOfflineType.OFFLINE);
    c.setStartDate(LocalDate.now(clock));
    c.setEndDate(LocalDate.now(clock).plusDays(7));
    assertThat(writer.upsert(c)).isEqualTo(ContestWriter.Result.ADDED);
    verify(mail, timeout(5000)).sendEvents(eq(u.getEmail()), contains("새 소식"), anyList());
    org.awaitility.Awaitility.await()
        .atMost(java.time.Duration.ofSeconds(5))
        .untilAsserted(() -> assertThat(logs.count()).isEqualTo(1));
  }

  @Test
  void nonexistentFavoriteIsRejected() {
    var u = user("favorite@example.test");
    assertThatThrownBy(() -> favoriteService.add(u.getId(), 999999L))
        .hasMessageContaining("찾을 수 없습니다");
  }

  @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
  @Autowired com.moamoa.service.contest.ContestQueryService queryService;

  @Test
  void favoriteStatusUsesOneBatchQueryRegardlessOfPageSize() {
    var u = user("queries@example.test");
    var c = event("small-page", ContestCategory.ART, 0, 5);
    favoriteService.add(u.getId(), c.getId());
    var statistics =
        entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    try {
      statistics.clear();
      var small = queryService.search(new ContestSearchCondition(), 0, u.getId());
      assertThat(small.favoriteIds()).containsExactly(c.getId());
      assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
      for (int i = 0; i < 15; i++) event("large-page-" + i, ContestCategory.ART, 0, 5);
      statistics.clear();
      assertThat(
              queryService.search(new ContestSearchCondition(), 0, u.getId()).page().getContent())
          .hasSize(12);
      assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    } finally {
      statistics.setStatisticsEnabled(false);
    }
  }
}
