package com.moamoa.client.mapper;

import com.moamoa.client.kocca.KoccaNotice;
import com.moamoa.domain.contest.*;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class KoccaContestMapper implements ExternalContestMapper<KoccaNotice> {
  public Optional<Contest> map(KoccaNotice raw) {
    if (raw.title() == null || raw.title().isBlank())
      throw new IllegalArgumentException("Missing KOCCA title");
    String link = raw.link() == null ? "" : raw.link().trim();
    if (link.startsWith("www.kocca.kr/") || link.startsWith("kocca.kr/")) link = "https://" + link;
    if (SeoulContestMapper.safeUrl(link) == null)
      throw new IllegalArgumentException("Invalid KOCCA link");
    var uri = URI.create(link);
    if (!Set.of("www.kocca.kr", "kocca.kr").contains(uri.getHost().toLowerCase(Locale.ROOT))
        || !"/kocca/pims/view.do".equals(uri.getPath()))
      throw new IllegalArgumentException("Invalid KOCCA origin");
    String id =
        Arrays.stream(Objects.toString(uri.getRawQuery(), "").split("&"))
            .filter(p -> p.startsWith("intcNo="))
            .map(p -> p.substring(7))
            .findFirst()
            .orElse("");
    if (!id.matches("[A-Za-z0-9-]{1,64}"))
      throw new IllegalArgumentException("Missing KOCCA identity");
    var c = new Contest();
    c.setSource(ContestSource.KOCCA);
    c.setSourceId(id);
    c.setTitle(limit(HtmlUtils.htmlUnescape(raw.title()), 1000));
    c.setDescription(plain(raw.content()));
    c.setOriginalUrl("https://www.kocca.kr/kocca/pims/view.do?intcNo=" + id + "&menuNo=204104");
    c.setOriginalCategory(limit(raw.cate(), 255));
    c.setCategory(ContestCategory.CONTEST);
    c.setType(ContestType.CONTEST);
    // The API does not supply a structured eligibility region or submission method.
    c.setRegion(ContestRegion.OTHER);
    c.setOnOffline(OnOfflineType.UNKNOWN);
    c.setStartDate(date(raw.startDt()));
    c.setEndDate(date(raw.endDt()));
    if (c.getEndDate().isBefore(c.getStartDate()))
      throw new IllegalArgumentException("Invalid KOCCA dates");
    return Optional.of(c);
  }

  private static LocalDate date(String value) {
    if (value == null || !value.matches("\\d{8}"))
      throw new IllegalArgumentException("Missing KOCCA date");
    return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
  }

  private static String limit(String value, int n) {
    return value == null ? null : value.substring(0, Math.min(value.length(), n));
  }

  private static String plain(String value) {
    if (value == null) return null;
    return HtmlUtils.htmlUnescape(value)
        .replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>", "")
        .replaceAll("(?i)<br\\s*/?>|</(?:p|div|li|tr)>", "\n")
        .replaceAll("<[^>]+>", "")
        .replace("\\n", "\n")
        .trim();
  }
}
