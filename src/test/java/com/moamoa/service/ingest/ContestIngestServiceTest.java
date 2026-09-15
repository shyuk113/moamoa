package com.moamoa.service.ingest;

import static org.mockito.Mockito.*;

import com.moamoa.client.ExternalContestClient;
import com.moamoa.domain.contest.*;
import com.moamoa.service.notification.EmailSenderService;
import java.util.*;
import org.junit.jupiter.api.Test;

class ContestIngestServiceTest {
  @Test
  void sourceFailuresAreIsolatedAndAlertAtThree() {
    var a = mock(ExternalContestClient.class);
    var b = mock(ExternalContestClient.class);
    var writer = mock(ContestWriter.class);
    var mail = mock(EmailSenderService.class);
    when(a.enabled()).thenReturn(true);
    when(b.enabled()).thenReturn(true);
    when(a.source()).thenReturn(ContestSource.KOCCA);
    when(b.source()).thenReturn(ContestSource.SEOUL_OPENAPI);
    when(a.fetch()).thenThrow(new IllegalStateException("bad schema"));
    var c = new Contest();
    when(b.fetch()).thenReturn(new ExternalContestClient.FetchResult(List.of(c), 1, 0, 0, 0));
    when(writer.upsert(c)).thenReturn(ContestWriter.Result.ADDED);
    var service = new ContestIngestService(List.of(a, b), writer, mail);
    service.syncAll();
    service.syncAll();
    verify(mail, never()).sendAdmin(anyString(), anyString());
    service.syncAll();
    verify(writer, times(3)).upsert(c);
    verify(mail).sendAdmin(anyString(), contains("3"));
    verify(b, times(3)).fetch();
  }
}
