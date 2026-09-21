package com.moamoa.dto;

import com.moamoa.domain.contest.*;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
public class ContestSearchCondition {
  private String keyword;
  private ContestCategory category;
  private ContestRegion region;
  private String district;
  private LocalDate from;
  private LocalDate to;
  private OnOfflineType onOffline;
  private boolean freeOnly;
}
