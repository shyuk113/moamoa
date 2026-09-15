package com.moamoa.service.ingest;

import com.moamoa.client.ExternalContestClient;
import com.moamoa.domain.contest.ContestSource;
import com.moamoa.service.notification.EmailSenderService;
import java.util.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;

@Service
public class ContestIngestService {
  private final List<ExternalContestClient> clients;
  private final ContestWriter writer;
  private final EmailSenderService mail;
  private final Map<ContestSource, Integer> failures = new EnumMap<>(ContestSource.class);
  private final Logger log = LoggerFactory.getLogger(getClass());

  public ContestIngestService(
      List<ExternalContestClient> clients, ContestWriter writer, EmailSenderService mail) {
    this.clients = clients;
    this.writer = writer;
    this.mail = mail;
  }

  public synchronized void syncAll() {
    for (var client : clients) {
      if (!client.enabled()) {
        log.info("Source {} disabled: credential missing", client.source());
        continue;
      }
      try {
        var fetched = client.fetch();
        int added = 0, updated = 0, skipped = fetched.skipped();
        for (var contest : fetched.contests())
          switch (writer.upsert(contest)) {
            case ADDED -> added++;
            case UPDATED -> updated++;
            case SKIPPED -> skipped++;
          }
        failures.put(client.source(), 0);
        log.info(
            "Sync {} scanned={} new={} updated={} skipped={} invalid={} identityFallbackRate={}%",
            client.source(),
            fetched.scanned(),
            added,
            updated,
            skipped,
            fetched.invalid(),
            fetched.contests().isEmpty()
                ? 0
                : 100.0 * fetched.fallback() / fetched.contests().size());
      } catch (RuntimeException ex) {
        int count = failures.merge(client.source(), 1, Integer::sum);
        // Never log client exceptions/URLs: the Seoul credential is in the request path.
        log.error(
            "Sync {} failed; consecutiveFailures={}; errorType={}",
            client.source(),
            count,
            ex.getClass().getSimpleName());
        if (count % 3 == 0)
          try {
            mail.sendAdmin("데이터 수집 실패", client.source() + " 수집이 " + count + "회 연속 실패했습니다.");
          } catch (RuntimeException ignored) {
            log.error("Admin notification failed");
          }
      }
    }
  }
}
