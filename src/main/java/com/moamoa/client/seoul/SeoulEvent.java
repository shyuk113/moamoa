package com.moamoa.client.seoul;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeoulEvent(
    @JsonProperty("CODENAME") String category,
    @JsonProperty("GUNAME") String district,
    @JsonProperty("TITLE") String title,
    @JsonProperty("PLACE") String place,
    @JsonProperty("STRTDATE") String start,
    @JsonProperty("END_DATE") String end,
    @JsonProperty("USE_TRGT") String eligibility,
    @JsonProperty("USE_FEE") String fee,
    @JsonProperty("IS_FREE") String isFree,
    @JsonProperty("PROGRAM") String program,
    @JsonProperty("ETC_DESC") String description,
    @JsonProperty("ORG_LINK") String originalUrl,
    @JsonProperty("HMPG_ADDR") String portalUrl,
    @JsonProperty("MAIN_IMG") String imageUrl,
    @JsonProperty("LAT") String latitude,
    @JsonProperty("LOT") String longitude,
    @JsonProperty("INQUIRY") String inquiry) {
  public SeoulEvent(
      String category,
      String district,
      String title,
      String place,
      String start,
      String end,
      String eligibility,
      String fee,
      String isFree,
      String program,
      String description,
      String originalUrl,
      String portalUrl,
      String imageUrl) {
    this(
        category,
        district,
        title,
        place,
        start,
        end,
        eligibility,
        fee,
        isFree,
        program,
        description,
        originalUrl,
        portalUrl,
        imageUrl,
        null,
        null,
        null);
  }
}
