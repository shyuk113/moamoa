package com.moamoa.client.mapper;

import static org.assertj.core.api.Assertions.*;

import com.moamoa.client.seoul.SeoulEvent;
import com.moamoa.domain.contest.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SeoulContestMapperTest {
  private final SeoulContestMapper mapper = new SeoulContestMapper();

  private SeoulEvent event(String category, String title, String link) {
    return new SeoulEvent(
        category,
        "종로구",
        title,
        "광화문",
        "2026-09-14 00:00:00.0",
        "2026-09-20 00:00:00.0",
        "누구나",
        "",
        "무료",
        "",
        "소개",
        "https://example.org",
        link,
        null);
  }

  @Test
  void fairTitleOverridesFestivalClassification() {
    var c =
        mapper
            .map(event("축제-기타", "서울도시농업박람회", "https://culture.seoul.go.kr/event?id=1"))
            .orElseThrow();
    assertThat(c.getCategory()).isEqualTo(ContestCategory.FAIR);
    assertThat(c.getType()).isEqualTo(ContestType.FAIR);
    assertThat(c.getDateLabel()).isEqualTo("행사 종료");
  }

  @Test
  void keepsExhibitionsAndSeasonalFestivals() {
    assertThat(
            mapper
                .map(event("전시/미술", "미술 전시", "https://example.org/1"))
                .orElseThrow()
                .getCategory())
        .isEqualTo(ContestCategory.ART);
    assertThat(
            mapper
                .map(event("축제-전통/역사", "추석 한마당", "https://example.org/2"))
                .orElseThrow()
                .getCategory())
        .isEqualTo(ContestCategory.FESTIVAL);
  }

  @Test
  void excludesUnrequestedConcertAndUnknown() {
    assertThat(mapper.map(event("콘서트", "재즈 공연", null))).isEmpty();
    assertThat(mapper.map(event("미지분류", "행사", null))).isEmpty();
  }

  @Test
  void stableIdentitySurvivesTitleCorrection() {
    assertThat(
            mapper.map(event("전시/미술", "수정전", "https://example.org/1")).orElseThrow().getSourceId())
        .isEqualTo(
            mapper.map(event("전시/미술", "수정후", "https://example.org/1")).orElseThrow().getSourceId());
  }

  @Test
  void unsafeOriginalLinksAreRejected() {
    assertThat(SeoulContestMapper.safeUrl("javascript:alert(1)")).isNull();
    assertThat(SeoulContestMapper.safeUrl("https://example.org/path"))
        .isEqualTo("https://example.org/path");
  }

  @Test
  void badgeUsesEventEndAndBoundaryDays() {
    var c = mapper.map(event("전시/미술", "미술", null)).orElseThrow();
    assertThat(c.dayBadge(LocalDate.of(2026, 9, 17))).isEqualTo("D-3");
    assertThat(c.dayBadge(LocalDate.of(2026, 9, 20))).isEqualTo("D-0");
    assertThat(c.dayBadge(LocalDate.of(2026, 9, 21))).isEqualTo("종료");
  }

  @Test
  void normalizersHaveExplicitFallback() {
    assertThat(new RegionNormalizer().normalize("서울특별시")).isEqualTo(ContestRegion.SEOUL);
    assertThat(new RegionNormalizer().normalize(null)).isEqualTo(ContestRegion.OTHER);
    assertThat(new CategoryNormalizer().normalize("unknown")).isEqualTo(ContestCategory.OTHER);
  }

  @Test
  void onlineAndHybridEventsAreNotClassifiedAsOffline() {
    assertThat(mapper.map(event("전시/미술", "온라인 전시", null)).orElseThrow().getOnOffline())
        .isEqualTo(OnOfflineType.ONLINE);
    assertThat(SeoulContestMapper.participation("온·오프라인 전시", "서울")).isEqualTo(OnOfflineType.BOTH);
    assertThat(SeoulContestMapper.participation("전시", null)).isEqualTo(OnOfflineType.UNKNOWN);
  }

  @Test
  void permanentExhibitionDoesNotDisplayAnArtificialDeadline() {
    var c = mapper.map(event("전시/미술", "상설 전시", null)).orElseThrow();
    c.setEndDate(LocalDate.of(2099, 12, 31));
    assertThat(c.dayBadge(LocalDate.of(2026, 9, 15))).isEqualTo("상설");
    assertThat(c.getPeriod()).endsWith("상설 운영");
    c.setType(ContestType.CONTEST);
    assertThat(c.isPermanent()).isFalse();
  }

  @Test
  void importsVerifiedLocationFieldsAndDiscardsInvalidCoordinatesOnly() throws Exception {
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    String payload =
        """
        {"CODENAME":"전시/미술","TITLE":"지도 테스트","PLACE":"서울","STRTDATE":"2026-09-01",
         "END_DATE":"2026-10-01","LAT":"37.5512","LOT":"127.1573","INQUIRY":"02-123-4567"}
        """;
    var c =
        mapper.map(json.readValue(payload, com.moamoa.client.seoul.SeoulEvent.class)).orElseThrow();
    assertThat(c.getLatitude()).isEqualTo(37.5512);
    assertThat(c.getLongitude()).isEqualTo(127.1573);
    assertThat(c.getContactPhone()).isEqualTo("02-123-4567");
    var invalid =
        mapper
            .map(
                json.readValue(
                    payload.replace("37.5512", "NaN"), com.moamoa.client.seoul.SeoulEvent.class))
            .orElseThrow();
    assertThat(invalid.getLatitude()).isNull();
    assertThat(invalid.getLongitude()).isNull();
    assertThat(invalid.getTitle()).isEqualTo("지도 테스트");
  }
}
