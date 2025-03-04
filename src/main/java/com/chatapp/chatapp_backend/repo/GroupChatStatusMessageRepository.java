package com.chatapp.chatapp_backend.repo;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import com.chatapp.chatapp_backend.entity.GroupChatStatusMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupChatStatusMessageRepository extends JpaRepository<GroupChatStatusMessage,Long> {
    GroupChatStatusMessage findByMessageIdAndReceiverId(Long messageId, Long receiverId);
    Long countByGroup_IdAndReceiver_IdAndStatusNot(Long groupId, Long receiverId, MessageStatus status);
    List<GroupChatStatusMessage> findByGroup_IdAndMessage_id (Long group_Id, Long message_id);
    List<GroupChatStatusMessage> findByReceiver_idAndStatus(Long receiver_id, MessageStatus status);
}
