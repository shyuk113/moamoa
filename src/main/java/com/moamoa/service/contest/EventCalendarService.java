package com.moamoa.service.contest;

import com.moamoa.domain.contest.Contest;
import com.moamoa.exception.AppException;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EventCalendarService {
  private final Clock clock;

  public EventCalendarService(Clock clock) {
    this.clock = clock;
  }

  public String calendar(Contest c) {
    if (c.isPermanent())
      throw new AppException(
          HttpStatus.BAD_REQUEST,
          "상설 행사는 방문 날짜를 직접 선택해주세요. / Choose a date for permanent exhibitions.");
    var lines = new java.util.ArrayList<String>();
    lines.addAll(
        java.util.List.of(
            "BEGIN:VCALENDAR",
            "VERSION:2.0",
            "PRODID:-//moamoa//Events//KO",
            "CALSCALE:GREGORIAN",
            "BEGIN:VEVENT",
            "UID:event-" + c.getId() + "@moamoa",
            "DTSTAMP:"
                + DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                    .withZone(ZoneOffset.UTC)
                    .format(clock.instant()),
            "DTSTART;VALUE=DATE:" + c.getStartDate().format(DateTimeFormatter.BASIC_ISO_DATE),
            "DTEND;VALUE=DATE:"
                + c.getEndDate().plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE),
            "SUMMARY:" + escape(c.getTitle()),
            "LOCATION:" + escape(c.getPlace()),
            "DESCRIPTION:"
                + escape(
                    (c.getType() == com.moamoa.domain.contest.ContestType.CONTEST
                            ? "접수 기간이며 신청이 아닙니다. Application period only; this does not submit an"
                                + " application.\n"
                            : "행사 운영 기간이며 예약이 아닙니다. Event dates only; this is not a reservation.\n")
                        + java.util.Objects.toString(c.getOriginalUrl(), "")),
            "END:VEVENT",
            "END:VCALENDAR"));
    return lines.stream()
            .map(EventCalendarService::fold)
            .collect(java.util.stream.Collectors.joining("\r\n"))
        + "\r\n";
  }

  static String escape(String s) {
    return java.util.Objects.toString(s, "")
        .replace("\\", "\\\\")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\n", "\\n")
        .replace(";", "\\;")
        .replace(",", "\\,");
  }

  static String fold(String s) {
    var out = new StringBuilder();
    int size = 0;
    for (int cp : s.codePoints().toArray()) {
      String ch = new String(Character.toChars(cp));
      int bytes = ch.getBytes(StandardCharsets.UTF_8).length;
      if (size + bytes > 75) {
        out.append("\r\n ");
        size = 1;
      }
      out.append(ch);
      size += bytes;
    }
    return out.toString();
  }
}
