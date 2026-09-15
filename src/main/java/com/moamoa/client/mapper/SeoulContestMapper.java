package com.moamoa.client.mapper;

import com.moamoa.client.seoul.SeoulEvent;
import com.moamoa.domain.contest.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class SeoulContestMapper implements ExternalContestMapper<SeoulEvent> {
  private final CategoryNormalizer categories = new CategoryNormalizer();

  public Optional<Contest> map(SeoulEvent raw) {
    var category = categories.classify(raw.category(), raw.title());
    if (category == ContestCategory.OTHER) return Optional.empty();
    if (raw.title() == null || raw.title().isBlank())
      throw new IllegalArgumentException("Missing Seoul title");
    var c = new Contest();
    c.setTitle(limit(raw.title(), 1000));
    c.setCategory(category);
    c.setType(
        switch (category) {
          case ART -> ContestType.EXHIBITION;
          case FAIR -> ContestType.FAIR;
          case FESTIVAL -> ContestType.FESTIVAL;
          default -> throw new IllegalArgumentException("Unsupported Seoul category");
        });
    c.setRegion(ContestRegion.SEOUL);
    c.setDistrict(limit(raw.district(), 255));
    c.setOriginalCategory(limit(raw.category(), 255));
    c.setStartDate(date(raw.start()));
    c.setEndDate(date(raw.end()));
    if (c.getEndDate().isBefore(c.getStartDate()))
      throw new IllegalArgumentException("Invalid Seoul event dates");
    c.setPlace(limit(raw.place(), 1000));
    c.setEligibility(limit(raw.eligibility(), 1000));
    c.setOnOffline(participation(raw.title(), raw.place()));
    c.setDescription(
        Objects.toString(raw.program(), "") + "\n" + Objects.toString(raw.description(), ""));
    c.setFee("무료".equals(raw.isFree()) ? "무료" : limit(raw.fee(), 255));
    String portal = safeUrl(raw.portalUrl()), original = safeUrl(raw.originalUrl());
    c.setOriginalUrl(portal != null ? portal : original);
    c.setImageUrl(safeUrl(raw.imageUrl()));
    c.setSource(ContestSource.SEOUL_OPENAPI);
    // The API exposes no ID. Prefer the portal's event-specific URL; fallback cannot identify
    // renamed events.
    String identity =
        portal != null ? portal : raw.title() + "|" + raw.place() + "|" + c.getStartDate();
    c.setSourceId(hash(identity));
    return Optional.of(c);
  }

  private static String limit(String value, int length) {
    return value == null ? null : value.substring(0, Math.min(value.length(), length));
  }

  static OnOfflineType participation(String title, String place) {
    String text =
        (Objects.toString(title, "") + " " + Objects.toString(place, "")).toLowerCase(Locale.ROOT);
    if (text.contains("온·오프라인")
        || text.contains("온/오프라인")
        || text.contains("온오프라인")
        || (text.contains("온라인") && text.contains("오프라인"))) return OnOfflineType.BOTH;
    if (text.contains("온라인") || text.contains("online")) return OnOfflineType.ONLINE;
    return place == null || place.isBlank() ? OnOfflineType.UNKNOWN : OnOfflineType.OFFLINE;
  }

  static LocalDate date(String value) {
    if (value == null || value.length() < 10)
      throw new IllegalArgumentException("Missing Seoul date");
    return LocalDate.parse(value.substring(0, 10));
  }

  public static String safeUrl(String value) {
    try {
      if (value == null || value.length() > 2000) return null;
      var uri = URI.create(value.trim());
      return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
              && uri.getHost() != null
              && uri.getUserInfo() == null
          ? uri.toString()
          : null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
