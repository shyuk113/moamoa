package com.moamoa.service.contest;

import com.moamoa.client.mapper.SeoulContestMapper;
import com.moamoa.domain.contest.*;
import com.moamoa.dto.EventGuideRequest;
import com.moamoa.exception.AppException;
import com.moamoa.repository.contest.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventGuideService {
  private final EventGuideRepository guides;
  private final ContestQueryService contests;
  private final Clock clock;

  public EventGuideService(EventGuideRepository guides, ContestQueryService contests, Clock clock) {
    this.guides = guides;
    this.contests = contests;
    this.clock = clock;
  }

  public EventGuide editable(Long id, String language) {
    contests.get(id);
    language(language);
    return guides
        .findByContestIdAndLanguage(id, language)
        .orElseGet(
            () -> {
              var g = new EventGuide();
              g.setContestId(id);
              g.setLanguage(language);
              return g;
            });
  }

  @Transactional
  public void save(Long id, String language, EventGuideRequest r, Long editor) {
    var g = editable(id, language);
    if ((r.latitude() == null) != (r.longitude() == null))
      throw bad("위도와 경도를 함께 입력해주세요. / Enter both coordinates.");
    String source = SeoulContestMapper.safeUrl(r.sourceUrl());
    if (source == null)
      throw bad("확인한 원본 페이지의 http(s) 주소가 필요합니다. / A valid source URL is required.");
    var images = images(r.gallery());
    if (images.size() > 8) throw bad("사진은 최대 8장입니다. / Maximum 8 images.");
    for (String image : images)
      if (SeoulContestMapper.safeUrl(image) == null || !image.startsWith("https://"))
        throw bad("사진 주소는 HTTPS여야 합니다. / Use HTTPS image URLs.");
    if ((!images.isEmpty() || text(r.videoId()) != null) && text(r.attribution()) == null)
      throw bad("미디어 출처와 사용 권한을 입력해주세요. / Media attribution and permission are required.");
    g.setTitle(text(r.title()));
    g.setDescription(text(r.description()));
    g.setPlace(text(r.place()));
    g.setFee(text(r.fee()));
    g.setEligibility(text(r.eligibility()));
    g.setTransit(text(r.transit()));
    g.setParking(text(r.parking()));
    g.setBooking(text(r.booking()));
    g.setAccessibility(text(r.accessibility()));
    g.setContactPhone(text(r.contactPhone()));
    g.setContactEmail(text(r.contactEmail()));
    g.setAddress(text(r.address()));
    g.setLatitude(r.latitude());
    g.setLongitude(r.longitude());
    g.setGallery(String.join("\n", images));
    g.setVideoId(text(r.videoId()));
    g.setAttribution(text(r.attribution()));
    g.setSourceUrl(source);
    g.setUpdatedBy(editor);
    g.setUpdatedAt(clock.instant());
    guides.save(g);
  }

  public record View(
      String title,
      String description,
      String place,
      String fee,
      String eligibility,
      String transit,
      String parking,
      String booking,
      String accessibility,
      String contactPhone,
      String contactEmail,
      String address,
      List<String> gallery,
      String videoUrl,
      String attribution,
      String sourceUrl,
      Instant updatedAt,
      String mapUrl,
      String mapSearchUrl,
      boolean originalLanguage) {}

  public View view(Contest c, Locale locale) {
    var base = guides.findByContestIdAndLanguage(c.getId(), "ko").orElse(new EventGuide());
    boolean english = "en".equals(locale.getLanguage());
    var local =
        english
            ? guides.findByContestIdAndLanguage(c.getId(), "en").orElse(new EventGuide())
            : base;
    String title = first(local.getTitle(), base.getTitle(), c.getTitle());
    String place = first(local.getPlace(), base.getPlace(), c.getPlace());
    Double lat = base.getLatitude() != null ? base.getLatitude() : c.getLatitude();
    Double lon = base.getLongitude() != null ? base.getLongitude() : c.getLongitude();
    String map = null;
    if (lat != null && lon != null)
      map =
          "https://www.openstreetmap.org/export/embed.html?bbox="
              + Math.max(-180, lon - .006)
              + ","
              + Math.max(-90, lat - .004)
              + ","
              + Math.min(180, lon + .006)
              + ","
              + Math.min(90, lat + .004)
              + "&layer=mapnik&marker="
              + lat
              + ","
              + lon;
    String query =
        lat != null && lon != null ? lat + "," + lon : first(base.getAddress(), c.getPlace());
    String search =
        query == null
            ? null
            : "https://www.google.com/maps/search/?api=1&query="
                + URLEncoder.encode(query, StandardCharsets.UTF_8);
    return new View(
        title,
        first(local.getDescription(), base.getDescription(), c.getDescription()),
        place,
        first(local.getFee(), base.getFee(), c.getFee()),
        first(local.getEligibility(), base.getEligibility(), c.getEligibility()),
        first(local.getTransit(), base.getTransit()),
        first(local.getParking(), base.getParking()),
        first(local.getBooking(), base.getBooking()),
        first(local.getAccessibility(), base.getAccessibility()),
        first(base.getContactPhone(), c.getContactPhone()),
        base.getContactEmail(),
        first(local.getAddress(), base.getAddress()),
        images(base.getGallery()),
        base.getVideoId() == null
            ? null
            : "https://www.youtube-nocookie.com/embed/" + base.getVideoId(),
        base.getAttribution(),
        first(local.getSourceUrl(), base.getSourceUrl(), c.getOriginalUrl()),
        local.getUpdatedAt() != null ? local.getUpdatedAt() : base.getUpdatedAt(),
        map,
        search,
        english);
  }

  public Map<Long, String> titles(List<Contest> events, Locale locale) {
    if (!"en".equals(locale.getLanguage()) || events.isEmpty()) return Map.of();
    var result = new HashMap<Long, String>();
    guides
        .findByContestIdInAndLanguage(events.stream().map(Contest::getId).toList(), "en")
        .forEach(
            g -> {
              if (text(g.getTitle()) != null) result.put(g.getContestId(), g.getTitle());
            });
    return result;
  }

  public static List<String> images(String s) {
    return s == null
        ? List.of()
        : s.lines().map(String::trim).filter(v -> !v.isEmpty()).distinct().toList();
  }

  private static String text(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  private static String first(String... values) {
    return Arrays.stream(values)
        .map(EventGuideService::text)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private static void language(String l) {
    if (!Set.of("ko", "en").contains(l)) throw bad("지원하지 않는 언어입니다. / Unsupported language.");
  }

  private static AppException bad(String s) {
    return new AppException(HttpStatus.BAD_REQUEST, s);
  }
}
