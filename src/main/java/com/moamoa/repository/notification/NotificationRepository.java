package com.moamoa.repository.notification;

import com.moamoa.domain.notification.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface NotificationRepository extends JpaRepository<NotificationLog, Long> {
  @Modifying
  @Query(
      value =
          "insert into notification_logs(user_id,contest_id,type,success)"
              + " values(:userId,:contestId,:type,false) on conflict(user_id,contest_id,type) do"
              + " nothing",
      nativeQuery = true)
  int claim(Long userId, Long contestId, String type);

  Optional<NotificationLog> findByUserIdAndContestIdAndType(
      Long userId, Long contestId, NotificationType type);

  List<NotificationLog> findByTypeAndSuccessFalse(NotificationType type);

  List<NotificationLog> findByUserIdAndTypeAndSuccessFalse(Long userId, NotificationType type);

  @Query("select distinct n.userId from NotificationLog n where n.type=:type and n.success=false")
  List<Long> findPendingUserIds(NotificationType type);
}
