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
    @JsonProperty("MAIN_IMG") String imageUrl) {}
