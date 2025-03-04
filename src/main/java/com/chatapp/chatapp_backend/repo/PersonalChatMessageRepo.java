package com.chatapp.chatapp_backend.repo;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import com.chatapp.chatapp_backend.entity.PersonalChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonalChatMessageRepo extends JpaRepository<PersonalChatMessage,Long> {
    List<PersonalChatMessage> findAllBySession_Id(Long SessionId);

    List<PersonalChatMessage> findBySession_IdAndReceiver_IdAndStatus(Long sessionId, Long receiverId, MessageStatus status);

    PersonalChatMessage findTopBySession_IdOrderBySendTimeDesc(Long sessionId);

    @Query("SELECT COUNT(m) FROM PersonalChatMessage m " +
            "WHERE m.receiver.id = :receiverId AND m.sender.id = :senderId " +
            "AND m.status <> 'READ'")
    Long countUnreadMessagesByReceiverIdAndSenderId(@Param("receiverId") Long receiverId,
                                                    @Param("senderId") Long senderId);

}
