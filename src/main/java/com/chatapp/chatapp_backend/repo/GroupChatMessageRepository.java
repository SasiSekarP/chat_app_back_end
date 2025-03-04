package com.chatapp.chatapp_backend.repo;

import com.chatapp.chatapp_backend.entity.GroupChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupChatMessageRepository extends JpaRepository<GroupChatMessage,Long> {

    @Query("SELECT DISTINCT gcm FROM GroupChatMessage gcm " +
            "LEFT JOIN gcm.messageStatus ms " +
            "WHERE gcm.group.id = :groupId " +
            "AND (gcm.sender.id = :userId OR ms.receiver.id = :userId)")
    List<GroupChatMessage> findByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);


    GroupChatMessage findTopByGroup_IdOrderBySendTimeDesc(Long group_id);
}
