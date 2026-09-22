package com.moamoa.client.kocca;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KoccaNotice(
    String title,
    String intcNoSeq,
    String cate,
    String regDt,
    String link,
    String startDt,
    String endDt,
    String content) {}
