package com.moamoa.client.mapper;

import com.moamoa.domain.contest.ContestCategory;
import java.util.LinkedHashMap;

public class CategoryNormalizer extends KeywordEnumNormalizer<ContestCategory> {
  public CategoryNormalizer() {
    super(keywords(), ContestCategory.OTHER);
  }

  private static LinkedHashMap<String, ContestCategory> keywords() {
    var m = new LinkedHashMap<String, ContestCategory>();
    m.put("박람회", ContestCategory.FAIR);
    m.put("엑스포", ContestCategory.FAIR);
    m.put("expo", ContestCategory.FAIR);
    m.put("전시/미술", ContestCategory.ART);
    m.put("축제", ContestCategory.FESTIVAL);
    return m;
  }

  // Title only promotes fairs; exhibitions keep their source classification.
  public ContestCategory classify(String originalCategory, String title) {
    var titleClass = normalize(title);
    return titleClass == ContestCategory.FAIR ? titleClass : normalize(originalCategory);
  }
}
