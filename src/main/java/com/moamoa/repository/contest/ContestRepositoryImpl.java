package com.moamoa.repository.contest;

import com.moamoa.domain.contest.*;
import com.moamoa.dto.ContestSearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.*;
import org.springframework.data.domain.*;

public class ContestRepositoryImpl implements ContestRepositoryCustom {
  private final JPAQueryFactory query;
  private final Clock clock;

  public ContestRepositoryImpl(EntityManager em, Clock clock) {
    query = new JPAQueryFactory(em);
    this.clock = clock;
  }

  public Page<Contest> search(ContestSearchCondition c, Pageable p) {
    var q = QContest.contest;
    var where = new BooleanBuilder(q.endDate.goe(LocalDate.now(clock)));
    if (c.getKeyword() != null && !c.getKeyword().isBlank())
      where.and(q.title.containsIgnoreCase(c.getKeyword().trim()));
    if (c.getCategory() != null) where.and(q.category.eq(c.getCategory()));
    if (c.getRegion() != null) where.and(q.region.eq(c.getRegion()));
    if (c.getDistrict() != null && !c.getDistrict().isBlank())
      where.and(q.district.eq(c.getDistrict()));
    if (c.getFrom() != null) where.and(q.endDate.goe(c.getFrom()));
    if (c.getTo() != null) where.and(q.startDate.loe(c.getTo()));
    if (c.getOnOffline() != null) where.and(q.onOffline.eq(c.getOnOffline()));
    if (c.isFreeOnly()) where.and(q.fee.eq("무료"));
    var content =
        query
            .selectFrom(q)
            .where(where)
            .orderBy(q.createdAt.desc(), q.id.desc())
            .offset(p.getOffset())
            .limit(p.getPageSize())
            .fetch();
    Long total = query.select(q.count()).from(q).where(where).fetchOne();
    return new PageImpl<>(content, p, total == null ? 0 : total);
  }
}
