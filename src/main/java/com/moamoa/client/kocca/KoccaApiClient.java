package com.moamoa.client.kocca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moamoa.client.ExternalContestClient;
import com.moamoa.client.mapper.KoccaContestMapper;
import com.moamoa.domain.contest.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KoccaApiClient implements ExternalContestClient {
  private static final int PAGE_SIZE = 100;
  private final String key;
  private final boolean configured;
  private final int maxPages;
  private final KoccaContestMapper mapper;
  private final HttpClient http;
  private final ObjectMapper json = new ObjectMapper();

  @org.springframework.beans.factory.annotation.Autowired
  public KoccaApiClient(
      @Value("${app.kocca-api-key:}") String key,
      @Value("${app.kocca-enabled:true}") boolean enabled,
      @Value("${app.kocca-max-pages:200}") int maxPages,
      KoccaContestMapper mapper) {
    this(
        key,
        enabled,
        maxPages,
        mapper,
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
  }

  KoccaApiClient(
      String key, boolean enabled, int maxPages, KoccaContestMapper mapper, HttpClient http) {
    this.key = key.trim();
    this.configured = enabled;
    this.maxPages = Math.max(1, Math.min(1000, maxPages));
    this.mapper = mapper;
    this.http = http;
  }

  public ContestSource source() {
    return ContestSource.KOCCA;
  }

  public boolean enabled() {
    return configured && !key.isBlank();
  }

  public FetchResult fetch() {
    if (!enabled()) return new FetchResult(List.of(), 0, 0, 0, 0);
    var results = new LinkedHashMap<String, Contest>();
    int scanned = 0, invalid = 0;
    // Closed notices also update previously imported rows (including early closure).
    for (boolean closed : new boolean[] {false, true}) {
      var seenPages = new HashSet<String>();
      boolean finished = false;
      for (int page = 1; page <= maxPages; page++) {
        try {
          var uri =
              URI.create(
                  "https://kocca.kr/api/pims/List.do?serviceKey="
                      + URLEncoder.encode(key, StandardCharsets.UTF_8)
                      + "&numOfRows="
                      + PAGE_SIZE
                      + "&pageNo="
                      + page
                      + (closed ? "&cate=4" : ""));
          var response =
              http.send(
                  HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build(),
                  HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
          if (response.statusCode() != 200) throw new IllegalStateException("KOCCA HTTP failure");
          if (response.body().length() > 20_000_000)
            throw new IllegalStateException("KOCCA response too large");
          var info = json.readTree(response.body()).path("INFO");
          String code = info.path("resultCode").asText();
          if ("INFO-200".equals(code)) {
            finished = true;
            break;
          }
          var rows = info.path("list");
          if (!"INFO-000".equals(code)
              || !rows.isArray()
              || info.path("pageNo").asInt(-1) != page
              || info.path("listCount").asInt(-1) != rows.size()
              || rows.size() > PAGE_SIZE)
            throw new IllegalStateException("KOCCA response schema/result invalid");
          String signature =
              UUID.nameUUIDFromBytes(rows.toString().getBytes(StandardCharsets.UTF_8)).toString();
          if (!seenPages.add(signature)) throw new IllegalStateException("KOCCA repeated page");
          for (var row : rows) {
            scanned++;
            try {
              var c = mapper.map(json.treeToValue(row, KoccaNotice.class)).orElseThrow();
              c.setSourceClosed(closed);
              results.put(c.getSourceId(), c);
            } catch (IllegalArgumentException
                | java.time.DateTimeException
                | com.fasterxml.jackson.core.JsonProcessingException ex) {
              invalid++;
            }
          }
          if (rows.size() < PAGE_SIZE) {
            finished = true;
            break;
          }
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException("KOCCA request interrupted");
        } catch (java.io.IOException ex) {
          // No exception cause: HTTP/JSON errors can contain credentials or response content.
          throw new IllegalStateException("KOCCA request failed");
        }
      }
      if (!finished) throw new IllegalStateException("KOCCA page limit reached; no partial import");
    }
    return new FetchResult(List.copyOf(results.values()), scanned, 0, invalid, 0);
  }
}
