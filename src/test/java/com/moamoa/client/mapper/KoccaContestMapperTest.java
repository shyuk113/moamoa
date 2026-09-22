package com.moamoa.client.mapper;

import static org.assertj.core.api.Assertions.*;

import com.moamoa.client.kocca.KoccaNotice;
import com.moamoa.domain.contest.*;
import org.junit.jupiter.api.Test;

class KoccaContestMapperTest {
  private final KoccaContestMapper mapper = new KoccaContestMapper();

  private KoccaNotice notice(String link, String start, String end) {
    return new KoccaNotice(
        "<2026년> 공모 &amp; 지원",
        "3-26-D000-010",
        "모집공고",
        "20260901",
        link,
        start,
        end,
        "<p>지원 대상</p><script>hidden()</script>안내\\n다음 줄");
  }

  @Test
  void sourceLinkIdentityIsStableAndDoesNotUseNonUniqueBusinessNumber() {
    var a =
        mapper
            .map(
                notice(
                    "www.kocca.kr/kocca/pims/view.do?intcNo=326D00070010&menuNo=204104",
                    "20260901",
                    "20261001"))
            .orElseThrow();
    var b =
        mapper
            .map(
                notice(
                    "http://kocca.kr/kocca/pims/view.do?menuNo=204104&intcNo=326D00089010",
                    "20260901",
                    "20261001"))
            .orElseThrow();
    assertThat(a.getSourceId()).isEqualTo("326D00070010").isNotEqualTo(b.getSourceId());
    assertThat(a.getOriginalUrl()).startsWith("https://www.kocca.kr/");
    assertThat(a.getTitle()).isEqualTo("<2026년> 공모 & 지원");
    assertThat(a.getDescription())
        .contains("지원 대상\n", "안내\n다음 줄")
        .doesNotContain("<p>", "hidden()");
    assertThat(a.getCategory()).isEqualTo(ContestCategory.CONTEST);
    assertThat(a.getType()).isEqualTo(ContestType.CONTEST);
    assertThat(a.getRegion()).isEqualTo(ContestRegion.OTHER);
    assertThat(a.getOnOffline()).isEqualTo(OnOfflineType.UNKNOWN);
    assertThat(a.getEligibility()).isNull();
  }

  @Test
  void invalidDatesAndUntrustedLinksAreRejected() {
    String valid = "https://www.kocca.kr/kocca/pims/view.do?intcNo=ABC";
    for (String start : new String[] {"", "20260230", "20261002"})
      assertThatThrownBy(() -> mapper.map(notice(valid, start, "20261001")))
          .isInstanceOf(RuntimeException.class);
    for (String link :
        new String[] {
          "javascript:bad",
          "https://evil.test/kocca/pims/view.do?intcNo=ABC",
          "https://www.kocca.kr/kocca/pims/view.do?intcNo=",
          "https://user@www.kocca.kr/kocca/pims/view.do?intcNo=ABC"
        })
      assertThatThrownBy(() -> mapper.map(notice(link, "20260901", "20261001")))
          .isInstanceOf(IllegalArgumentException.class);
  }
}
