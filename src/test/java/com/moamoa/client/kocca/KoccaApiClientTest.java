package com.moamoa.client.kocca;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.moamoa.client.mapper.KoccaContestMapper;
import java.io.IOException;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KoccaApiClientTest {
  @SuppressWarnings("unchecked")
  private HttpResponse<String> response(String body) {
    return mock(
        HttpResponse.class,
        invocation ->
            switch (invocation.getMethod().getName()) {
              case "statusCode" -> 200;
              case "body" -> body;
              default -> org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
            });
  }

  private String row(String id, String end) {
    return "{\"title\":\"지원"
               + " 공고\",\"intcNoSeq\":\"shared\",\"cate\":\"자유공모\",\"link\":\"www.kocca.kr/kocca/pims/view.do?intcNo="
        + id
        + "\",\"startDt\":\"20260901\",\"endDt\":\""
        + end
        + "\"}";
  }

  private String page(int n, java.util.List<String> rows) {
    return "{\"INFO\":{\"resultCode\":\"INFO-000\",\"pageNo\":"
        + n
        + ",\"listCount\":"
        + rows.size()
        + ",\"list\":["
        + String.join(",", rows)
        + "]}}";
  }

  private String empty() {
    return "{\"INFO\":{\"resultCode\":\"INFO-200\"}}";
  }

  private KoccaApiClient client(HttpClient http, int max) {
    return new KoccaApiClient("private+key=", true, max, new KoccaContestMapper(), http);
  }

  @Test
  void paginatesUsingPageLengthAndFetchesClosedUpdates() throws Exception {
    var http = mock(HttpClient.class);
    var rows =
        java.util.stream.IntStream.range(0, 100).mapToObj(i -> row("ID" + i, "20261001")).toList();
    when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(
            response(page(1, rows)),
            response(page(2, java.util.List.of(row("NEXT", "20261001")))),
            response(page(1, java.util.List.of(row("ID0", "20261001")))));
    var result = client(http, 5).fetch();
    assertThat(result.scanned()).isEqualTo(102);
    assertThat(result.contests()).hasSize(101);
    assertThat(result.contests().getFirst().isSourceClosed()).isTrue();
    var requests = ArgumentCaptor.forClass(HttpRequest.class);
    verify(http, times(3)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
    assertThat(requests.getAllValues().get(1).uri().toString())
        .contains("pageNo=2", "serviceKey=private%2Bkey%3D");
    assertThat(requests.getAllValues().get(2).uri().toString()).contains("cate=4");
  }

  @Test
  void invalidRowsAreIsolatedAndEmptyResponseIsValid() throws Exception {
    var http = mock(HttpClient.class);
    when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(
            response(page(1, java.util.List.of(row("OK", "20261001"), row("BAD", "20261399")))),
            response(empty()));
    var result = client(http, 5).fetch();
    assertThat(result.invalid()).isEqualTo(1);
    assertThat(result.contests()).hasSize(1);
  }

  @Test
  void disabledClientDoesNotSendRequests() {
    var http = mock(HttpClient.class);
    assertThat(new KoccaApiClient("", true, 5, new KoccaContestMapper(), http).enabled()).isFalse();
    var c = new KoccaApiClient("key", false, 5, new KoccaContestMapper(), http);
    assertThat(c.fetch().scanned()).isZero();
    verifyNoInteractions(http);
  }

  @Test
  void errorsAndPartialPaginationFailWithoutCredentialInException() throws Exception {
    for (String payload :
        java.util.List.of(
            "{\"INFO\":{\"resultCode\":\"INFO-100\"}}", "{}", "not json private+key=")) {
      var http = mock(HttpClient.class);
      when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
          .thenReturn(response(payload));
      assertThatThrownBy(() -> client(http, 5).fetch())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageNotContaining("private+key=")
          .hasNoCause();
    }
    var http = mock(HttpClient.class);
    when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new IOException("URL includes private+key="));
    assertThatThrownBy(() -> client(http, 5).fetch())
        .hasMessage("KOCCA request failed")
        .hasNoCause();
    var full =
        java.util.stream.IntStream.range(0, 100).mapToObj(i -> row("ID" + i, "20261001")).toList();
    reset(http);
    when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(response(page(1, full)));
    assertThatThrownBy(() -> client(http, 1).fetch()).hasMessageContaining("page limit");
    reset(http);
    when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(response(page(1, full)), response(page(2, full)));
    assertThatThrownBy(() -> client(http, 5).fetch()).hasMessageContaining("repeated page");
  }
}
