package com.moamoa.client.mapper;

import java.util.*;

public abstract class KeywordEnumNormalizer<T extends Enum<T>> {
  private final Map<String, T> keywords;
  private final T fallback;

  protected KeywordEnumNormalizer(Map<String, T> keywords, T fallback) {
    this.keywords = new LinkedHashMap<>(keywords);
    this.fallback = fallback;
  }

  public T normalize(String text) {
    if (text != null)
      for (var entry : keywords.entrySet())
        if (text.toLowerCase(Locale.ROOT).contains(entry.getKey().toLowerCase(Locale.ROOT)))
          return entry.getValue();
    return fallback;
  }
}
