package com.moamoa.domain.contest;

public enum ContestCategory {
  CONTEST("공모전"),
  ART("전시·미술"),
  FAIR("박람회"),
  FESTIVAL("지역·계절 축제"),
  OTHER("기타");
  private final String label;

  ContestCategory(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
