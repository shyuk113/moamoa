package com.moamoa.client.seoul;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamoa.client.ExternalContestClient;
import com.moamoa.client.mapper.SeoulContestMapper;
import com.moamoa.domain.contest.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SeoulOpenApiClient implements ExternalContestClient {
  private final String key;
  private final SeoulContestMapper mapper;
  private final Clock clock;
  private final HttpClient http;
  private final ObjectMapper json = new ObjectMapper();

  @org.springframework.beans.factory.annotation.Autowired
  public SeoulOpenApiClient(
      @Value("${app.seoul-api-key}") String key, SeoulContestMapper mapper, Clock clock) {
    this(
        key, mapper, clock, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }

  SeoulOpenApiClient(String key, SeoulContestMapper mapper, Clock clock, HttpClient http) {
    this.key = key;
    this.mapper = mapper;
    this.clock = clock;
    this.http = http;
  }

  public ContestSource source() {
    return ContestSource.SEOUL_OPENAPI;
  }

  public boolean enabled() {
    return !key.isBlank();
  }

  public FetchResult fetch() {
    var results = new ArrayList<Contest>();
    int scanned = 0, skipped = 0, invalid = 0, fallback = 0, total = 1;
    for (int start = 1; start <= total; start += 1000) {
      try {
        var uri =
            URI.create(
                "http://openapi.seoul.go.kr:8088/"
                    + URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8)
                    + "/json/culturalEventInfo/"
                    + start
                    + "/"
                    + (start + 999)
                    + "/");
        var response =
            http.send(
                HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
          throw new IllegalStateException("Seoul API HTTP status " + response.statusCode());
        var root = json.readTree(response.body());
        var body = root.path("culturalEventInfo");
        var code =
            body.path("RESULT").path("CODE").asText(root.path("RESULT").path("CODE").asText());
        if ("INFO-200".equals(code)) break;
        if (!"INFO-000".equals(code)
            || !body.path("row").isArray()
            || !body.path("list_total_count").canConvertToInt())
          throw new IllegalStateException("Seoul API response schema/result invalid");
        total = body.path("list_total_count").asInt();
        if (total > 100000) throw new IllegalStateException("Seoul API total exceeds safety bound");
        if (body.path("row").isEmpty() && start <= total)
          throw new IllegalStateException("Seoul API unexpected empty page");
        for (var node : body.path("row")) {
          scanned++;
          try {
            var raw = json.treeToValue(node, SeoulEvent.class);
            var mapped = mapper.map(raw);
            if (mapped.isEmpty()) {
              skipped++;
              continue;
            }
            var c = mapped.get();
            if (raw.portalUrl() == null || SeoulContestMapper.safeUrl(raw.portalUrl()) == null)
              fallback++;
            results.add(c);
          } catch (IllegalArgumentException
              | java.time.DateTimeException
              | com.fasterxml.jackson.core.JsonProcessingException e) {
            invalid++;
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Seoul API interrupted");
      } catch (java.io.IOException e) {
        throw new IllegalStateException("Seoul API transport/JSON failure");
      }
    }
    if (invalid > 0 && invalid == scanned - skipped)
      throw new IllegalStateException("All selected Seoul rows failed validation");
    return new FetchResult(results, scanned, skipped, invalid, fallback);
  }
}
