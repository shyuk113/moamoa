package com.moamoa.client.seoul;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.moamoa.client.mapper.SeoulContestMapper;
import java.net.http.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class SeoulOpenApiClientTest {
  @SuppressWarnings("unchecked")
  @Test
  void invalidDateIsIsolatedAndEarlyEndedUpdatesReachWriter() throws Exception {
    var http = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body())
        .thenReturn(
            """
            {"culturalEventInfo":{"list_total_count":3,"RESULT":{"CODE":"INFO-000"},"row":[
              {"CODENAME":"전시/미술","TITLE":"정상","STRTDATE":"2026-09-01","END_DATE":"2026-09-20"},
              {"CODENAME":"전시/미술","TITLE":"날짜 오류","STRTDATE":"2026-99-01","END_DATE":"2026-09-20"},
              {"CODENAME":"전시/미술","TITLE":"조기 종료","STRTDATE":"2026-09-01","END_DATE":"2026-09-10"}
            ]}}
            """);
    when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(response);
    var client =
        new SeoulOpenApiClient(
            "test",
            new SeoulContestMapper(),
            Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC),
            http);
    var result = client.fetch();
    assertThat(result.invalid()).isEqualTo(1);
    assertThat(result.contests()).extracting(c -> c.getTitle()).containsExactly("정상", "조기 종료");
  }

  @SuppressWarnings("unchecked")
  @Test
  void schemaMismatchFailsLoudlyWithoutLeakingKey() throws Exception {
    var http = mock(HttpClient.class);
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(200);
    when(response.body()).thenReturn("{\"unexpected\":true}");
    when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(response);
    var client =
        new SeoulOpenApiClient(
            "secret-not-for-logs", new SeoulContestMapper(), Clock.systemUTC(), http);
    assertThatThrownBy(client::fetch)
        .hasMessageContaining("schema")
        .hasMessageNotContaining("secret-not-for-logs");
  }
}
