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
  @Autowired org.springframework.security.web.SecurityFilterChain securityFilterChain;
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
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
  @Autowired EventGuideRepository guides;
  @Autowired EventReviewRepository reviews;
  @Autowired com.moamoa.service.contest.EventGuideService guideService;
  @Autowired com.moamoa.service.contest.EventReviewService reviewService;
  @MockitoBean EmailSenderService mail;

  @BeforeEach
  void clean() {
    // csrf() replaces the shared filter repository with a session-based test repository.
    // Restore production cookie behavior so browser-flow coverage is order independent.
    securityFilterChain.getFilters().stream()
        .filter(org.springframework.security.web.csrf.CsrfFilter.class::isInstance)
        .map(org.springframework.security.web.csrf.CsrfFilter.class::cast)
        .forEach(
            filter ->
                org.springframework.test.util.ReflectionTestUtils.setField(
                    filter,
                    "tokenRepository",
                    org.springframework.security.web.csrf.CookieCsrfTokenRepository
                        .withHttpOnlyFalse()));
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

  private Cookie access(User u) {
    return new Cookie("ACCESS_TOKEN", tokens.issue(u));
  }

  @Test
  void visitorGuidesRequireAdminAndSurviveSourceUpdates() throws Exception {
    var c = event("guide", ContestCategory.ART, 0, 7);
    var ordinary = user("ordinary@example.test");
    var admin = user("admin@example.test");
    admin.setRole(Role.ADMIN);
    users.saveAndFlush(admin);
    String endpoint = "/api/admin/contests/" + c.getId() + "/guide/ko";
    String body =
        """
        {"transit":"지하철 1번 출구","parking":"주차 불가","accessibility":"경사로 있음",
         "latitude":37.57,"longitude":126.98,"sourceUrl":"https://example.test/official",
         "gallery":"https://example.test/image.jpg","videoId":"abcdefghijk","attribution":"Organizer, used with permission"}
        """;
    mvc.perform(
            put(endpoint)
                .cookie(access(ordinary))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(put(endpoint).cookie(access(admin)).contentType("application/json").content(body))
        .andExpect(status().isForbidden());
    mvc.perform(
            put(endpoint)
                .cookie(access(admin))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk());
    c.setTitle("Source updated");
    writer.upsert(c);
    assertThat(guides.findByContestIdAndLanguage(c.getId(), "ko").orElseThrow().getParking())
        .isEqualTo("주차 불가");
    mvc.perform(get("/contests/" + c.getId()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("지하철 1번 출구")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString("openstreetmap.org/export/embed.html")));
    mvc.perform(get("/admin/contests/" + c.getId() + "/guide").cookie(access(admin)))
        .andExpect(status().isOk());
    mvc.perform(
            put(endpoint)
                .cookie(access(admin))
                .with(csrf())
                .contentType("application/json")
                .content(body.replace("https://example.test/image.jpg", "javascript:alert(1)")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put(endpoint)
                .cookie(access(admin))
                .with(csrf())
                .contentType("application/json")
                .content(body.replace("37.57", "91")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put(endpoint)
                .cookie(access(admin))
                .with(csrf())
                .contentType("application/json")
                .content(body.replace("Organizer, used with permission", "")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void englishPreferenceAndReviewedTranslationRenderWithFallback() throws Exception {
    var c = event("english", ContestCategory.ART, 0, 7);
    var admin = user("translation@example.test");
    admin.setRole(Role.ADMIN);
    users.saveAndFlush(admin);
    mvc.perform(
            put("/api/admin/contests/" + c.getId() + "/guide/en")
                .cookie(access(admin))
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"title\":\"Seoul art walk\",\"sourceUrl\":\"https://example.test/en\"}"))
        .andExpect(status().isOk());
    var response =
        mvc.perform(get("/contests/" + c.getId()).param("lang", "en"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Seoul art walk")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Plan your visit")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Not provided.")))
            .andReturn();
    Cookie language = response.getResponse().getCookie("MOAMOA_LANG");
    assertThat(language).isNotNull();
    var search = new ContestSearchCondition();
    search.setKeyword("Seoul art");
    assertThat(queryService.search(search, 0, null).page().getContent())
        .extracting(Contest::getId)
        .containsExactly(c.getId());
    for (String path : List.of("/mypage/favorites", "/mypage/interests"))
      mvc.perform(get(path).cookie(language, access(admin))).andExpect(status().isOk());
    mvc.perform(get("/contests").cookie(language))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Seoul art walk")));
    for (String path :
        List.of(
            "/", "/auth/login", "/auth/signup", "/auth/forgot-password", "/auth/reset-password"))
      mvc.perform(get(path).cookie(language))
          .andExpect(status().isOk())
          .andExpect(content().string(org.hamcrest.Matchers.containsString("Explore events")));
    mvc.perform(get("/contests/" + c.getId()).param("lang", "zz")).andExpect(status().isOk());
  }

  @Test
  void reviewsAreModeratedRateLimitedEscapedAndOwnerScoped() throws Exception {
    var c = event("reviews", ContestCategory.ART, -1, 7);
    var author = user("author@example.test");
    var other = user("other@example.test");
    var admin = user("moderator@example.test");
    admin.setRole(Role.ADMIN);
    users.saveAndFlush(admin);
    String path = "/api/reviews/events/" + c.getId();
    String body = "{\"rating\":5,\"body\":\"<script>alert(1)</script> Great exhibition\"}";
    mvc.perform(put(path).with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            put(path)
                .cookie(access(author))
                .with(csrf())
                .contentType("application/json")
                .content(body.replace("5", "6")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put(path)
                .cookie(access(author))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk());
    var r = reviews.findByContestIdAndUserId(c.getId(), author.getId()).orElseThrow();
    assertThat(reviews.stats(c.getId()).getCount()).isZero();
    mvc.perform(
            put(path)
                .cookie(access(author))
                .with(csrf())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isTooManyRequests());
    mvc.perform(delete("/api/reviews/" + r.getId()).cookie(access(other)).with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/admin/reviews/" + r.getId())
                .cookie(access(other))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"APPROVED\",\"version\":" + r.getVersion() + "}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/admin/reviews").cookie(access(admin))).andExpect(status().isOk());
    mvc.perform(
            put("/api/admin/reviews/" + r.getId())
                .cookie(access(admin))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"APPROVED\",\"version\":" + r.getVersion() + "}"))
        .andExpect(status().isOk());
    assertThat(reviews.stats(c.getId()).getAverage()).isEqualTo(5.0);
    mvc.perform(get("/contests/" + c.getId()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("<script>alert(1)"))));
    reviewService.moderate(
        r.getId(),
        EventReview.Status.HIDDEN,
        reviews.findById(r.getId()).orElseThrow().getVersion());
    assertThat(reviews.stats(c.getId()).getCount()).isZero();
    mvc.perform(delete("/api/reviews/" + r.getId()).cookie(access(author)).with(csrf()))
        .andExpect(status().isOk());
    assertThat(reviews.findById(r.getId())).isEmpty();
  }

  @Test
  void editedPublishedReviewRequiresFreshModerationVersion() throws Exception {
    var c = event("review-version", ContestCategory.ART, 0, 7);
    var author = user("version-author@example.test");
    var admin = user("version-admin@example.test");
    admin.setRole(Role.ADMIN);
    users.saveAndFlush(admin);
    reviewService.save(c.getId(), author.getId(), 5, "Originally reviewed text");
    var review = reviews.findByContestIdAndUserId(c.getId(), author.getId()).orElseThrow();
    reviewService.moderate(review.getId(), EventReview.Status.APPROVED, review.getVersion());
    assertThat(reviews.stats(c.getId()).getAverage()).isEqualTo(5.0);
    long staleVersion = reviews.findById(review.getId()).orElseThrow().getVersion();
    mvc.perform(get("/admin/reviews").param("status", "APPROVED").cookie(access(admin)))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"version\"")));
    jdbc.update(
        "update event_reviews set updated_at=? where id=?",
        java.sql.Timestamp.from(clock.instant().minusSeconds(120)),
        review.getId());
    mvc.perform(
            put("/api/reviews/events/" + c.getId())
                .cookie(access(author))
                .with(csrf())
                .contentType("application/json")
                .content("{\"rating\":1,\"body\":\"Changed and not reviewed\"}"))
        .andExpect(status().isOk());
    assertThat(reviews.stats(c.getId()).getCount()).isZero();
    assertThat(reviews.stats(c.getId()).getAverage()).isNull();
    mvc.perform(
            put("/api/admin/reviews/" + review.getId())
                .cookie(access(admin))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"APPROVED\",\"version\":" + staleVersion + "}"))
        .andExpect(status().isConflict());
    var changed = reviews.findById(review.getId()).orElseThrow();
    assertThat(changed.getStatus()).isEqualTo(EventReview.Status.PENDING);
    mvc.perform(get("/contests/" + c.getId()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Changed and not reviewed"))));
    mvc.perform(
            put("/api/admin/reviews/" + review.getId())
                .cookie(access(admin))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"APPROVED\",\"version\":" + changed.getVersion() + "}"))
        .andExpect(status().isOk());
    assertThat(reviews.stats(c.getId()).getAverage()).isEqualTo(1.0);
  }

  @Test
  void relatedEventsExcludeEndedAndOtherCategoriesAndCalendarUsesExclusiveEnd() throws Exception {
    var c = event("recommend", ContestCategory.ART, 0, 4);
    var neighbor = event("neighbor", ContestCategory.ART, 0, 5);
    event("ended", ContestCategory.ART, -3, -1);
    event("different", ContestCategory.FAIR, 0, 3);
    assertThat(
            contests.related(
                c.getId(),
                c.getCategory(),
                c.getDistrict(),
                LocalDate.now(clock),
                org.springframework.data.domain.PageRequest.of(0, 4)))
        .extracting(Contest::getId)
        .containsExactly(neighbor.getId());
    c.setTitle("전시;테스트,한글".repeat(20) + "\r\nBEGIN:BAD");
    contests.saveAndFlush(c);
    var response =
        mvc.perform(get("/contests/" + c.getId() + "/calendar.ics"))
            .andExpect(status().isOk())
            .andExpect(
                header()
                    .string(
                        "Content-Disposition", "attachment; filename=moamoa-" + c.getId() + ".ics"))
            .andReturn()
            .getResponse()
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(response)
        .contains(
            "DTEND;VALUE=DATE:"
                + c.getEndDate()
                    .plusDays(1)
                    .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE))
        .doesNotContain("\r\nBEGIN:BAD");
    for (String line : response.split("\r\n"))
      assertThat(line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
          .isLessThanOrEqualTo(75);
    c.setEndDate(LocalDate.of(2099, 12, 31));
    contests.saveAndFlush(c);
    mvc.perform(get("/contests/" + c.getId() + "/calendar.ics")).andExpect(status().isBadRequest());
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
