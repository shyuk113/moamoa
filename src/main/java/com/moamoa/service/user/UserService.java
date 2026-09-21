package com.moamoa.service.user;

import com.moamoa.domain.contest.ContestCategory;
import com.moamoa.domain.user.*;
import com.moamoa.repository.user.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository users;
  private final InterestRepository interests;

  public UserService(UserRepository users, InterestRepository interests) {
    this.users = users;
    this.interests = interests;
  }

  @Transactional(readOnly = true)
  public Set<ContestCategory> interests(Long userId) {
    var result = new HashSet<ContestCategory>();
    interests.findByUserId(userId).forEach(i -> result.add(i.getCategory()));
    return result;
  }

  @Transactional(readOnly = true)
  public NotificationFrequency frequency(Long userId) {
    return users.findById(userId).orElseThrow().getNotificationFrequency();
  }

  @Transactional
  public void update(
      Long userId, Set<ContestCategory> categories, NotificationFrequency frequency) {
    var u = users.lockById(userId).orElseThrow();
    interests.deleteByUserId(userId);
    interests.flush();
    interests.saveAll(categories.stream().map(c -> new InterestCategory(userId, c)).toList());
    u.setNotificationFrequency(frequency);
  }
}
