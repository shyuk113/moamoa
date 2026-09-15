package com.moamoa.client.mapper;

import com.moamoa.domain.contest.ContestRegion;
import java.util.Map;

public class RegionNormalizer extends KeywordEnumNormalizer<ContestRegion> {
  public RegionNormalizer() {
    super(Map.of("서울", ContestRegion.SEOUL, "전국", ContestRegion.NATIONWIDE), ContestRegion.OTHER);
  }
}
