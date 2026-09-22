package com.moamoa.service.contest;

import com.moamoa.domain.contest.*;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component("eventLabels")
public class EventLabels {
  public String district(String value, Locale locale) {
    if (value == null || value.isBlank()) return msg("site.seoul", locale);
    if (!"en".equals(locale.getLanguage())) return value;
    String[] ko = {
      "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구",
      "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"
    };
    String[] en = {
      "Gangnam-gu",
      "Gangdong-gu",
      "Gangbuk-gu",
      "Gangseo-gu",
      "Gwanak-gu",
      "Gwangjin-gu",
      "Guro-gu",
      "Geumcheon-gu",
      "Nowon-gu",
      "Dobong-gu",
      "Dongdaemun-gu",
      "Dongjak-gu",
      "Mapo-gu",
      "Seodaemun-gu",
      "Seocho-gu",
      "Seongdong-gu",
      "Seongbuk-gu",
      "Songpa-gu",
      "Yangcheon-gu",
      "Yeongdeungpo-gu",
      "Yongsan-gu",
      "Eunpyeong-gu",
      "Jongno-gu",
      "Jung-gu",
      "Jungnang-gu"
    };
    for (int i = 0; i < ko.length; i++) if (ko[i].equals(value)) return en[i];
    return value;
  }

  private final MessageSource messages;

  public EventLabels(MessageSource messages) {
    this.messages = messages;
  }

  private String msg(String key, Locale locale) {
    return messages.getMessage(key, null, locale);
  }

  public String category(ContestCategory c, Locale l) {
    return msg("category." + c.name(), l);
  }

  public String period(Contest c, Locale l) {
    return c.getStartDate()
        + " — "
        + (c.isPermanent() ? msg("period.permanent", l) : c.getEndDate());
  }

  public String badge(Contest c, LocalDate today, Locale l) {
    return c.isPermanent()
        ? msg("badge.permanent", l)
        : c.getEndDate().isBefore(today) ? msg("badge.ended", l) : c.dayBadge(today);
  }

  public String dateLabel(Contest c, Locale l) {
    return msg(
        c.isPermanent()
            ? "date.permanent"
            : c.getType() == ContestType.CONTEST ? "date.deadline" : "date.end",
        l);
  }
}
