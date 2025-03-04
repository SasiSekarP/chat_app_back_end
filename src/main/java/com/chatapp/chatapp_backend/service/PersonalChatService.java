package com.chatapp.chatapp_backend.service;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import com.chatapp.chatapp_backend.dto.GroupChatMessageDto;
import com.chatapp.chatapp_backend.dto.PersonalChatMessageDto;
import com.chatapp.chatapp_backend.entity.*;
import com.chatapp.chatapp_backend.repo.*;
import com.chatapp.chatapp_backend.websocket.ChatHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PersonalChatService {
    private final PersonalChatSessionRepo personalSessionRepo;
    private final UserRepository userRepository;
    private final PersonalChatMessageRepo personalChatMessageRepo;
    private final GroupChatMessageRepository groupChatMessageRepository;
    private final GroupRepository groupRepo;
    private final GroupChatStatusMessageRepository groupChatStatusMessageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final ConcurrentHashMap<String, WebSocketSession> users = ChatHandler.users;


    public Optional<PersonalChatSession> findPersonalChatSession(Long user1id, Long user2id) {
        // Ensure user1id is always the larger ID
        if (user2id > user1id) {
            Long temp = user2id;
            user2id = user1id;
            user1id = temp;
        }

        // Check if session already exists
        Optional<PersonalChatSession> personalChatSession = personalSessionRepo.findByUser1_IdAndUser2_Id(user1id, user2id);
        if (personalChatSession.isPresent()) {
            return personalChatSession;
        }

        // Fetch user entities
        Long finalUser1id = user1id;
        User user1 = userRepository.findById(user1id)
                .orElseThrow(() -> new RuntimeException("User with ID " + finalUser1id + " not found"));

        Long finalUser2id = user2id;
        User user2 = userRepository.findById(user2id)
                .orElseThrow(() -> new RuntimeException("User with ID " + finalUser2id + " not found"));

        // Create new session
        PersonalChatSession personalChatSessionEntity = new PersonalChatSession();
        personalChatSessionEntity.setUser1(user1);
        personalChatSessionEntity.setUser2(user2);

        return Optional.of(personalSessionRepo.save(personalChatSessionEntity));
    }

    public ResponseEntity<?> findPersonalChats(Long sessionid,Long senderid){
        List<PersonalChatMessage> chatList = personalChatMessageRepo.findAllBySession_Id(sessionid);
        this.changeMassageStatus(sessionid, senderid, MessageStatus.RECEIVED,MessageStatus.READ);
        return ResponseEntity.ok(Map.of("chatList",chatList,"sessionid",sessionid));
    }

    public void changeMassageStatus(Long sessionId, Long receiverId, MessageStatus fromMessageStatus, MessageStatus toMessageStatus) {
        List<PersonalChatMessage> messages = personalChatMessageRepo.findBySession_IdAndReceiver_IdAndStatus(sessionId, receiverId, fromMessageStatus);

        messages.forEach(message -> {
            message.setStatus(toMessageStatus);
            if (fromMessageStatus == MessageStatus.SENT) message.setReceivedTime(Instant.now());
            else message.setReadTime(Instant.now());
        });

        List<PersonalChatMessage> personalChatMessageEntityList = personalChatMessageRepo.saveAll(messages);
        personalChatMessageEntityList.forEach(message->{
            WebSocketSession senderSession = users.get(String.valueOf(message.getSender().getId()));
            if(senderSession != null && senderSession.isOpen()){
                try {
                    senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(ResponseEntity.ok(message))));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public PersonalChatMessage findLatestPersonalChat(Long sessionId) {
        return personalChatMessageRepo.findTopBySession_IdOrderBySendTimeDesc(sessionId);
    }

    public Long getUnreadMessageCount(Long userId,Long senderId) {
        return personalChatMessageRepo.countUnreadMessagesByReceiverIdAndSenderId(userId,senderId);
    }

    @Transactional
    public ResponseEntity<?> savePersonalChatMessage(PersonalChatMessageDto personalChatMessageDto) {

        PersonalChatMessage personalChatMessageEntity;

        if (personalChatMessageDto.getId() != null) {
            // Fetch existing message before updating
            personalChatMessageEntity = personalChatMessageRepo.findById(personalChatMessageDto.getId())
                    .orElseThrow(() -> new RuntimeException("Message not found for update"));

            // Update necessary fields
            personalChatMessageEntity.setReadTime(personalChatMessageDto.getReadTime());
            personalChatMessageEntity.setStatus(personalChatMessageDto.getStatus());
        } else {
            // Create new message if no ID is provided
            personalChatMessageEntity = new PersonalChatMessage();
            personalChatMessageEntity.setReceivedTime(personalChatMessageDto.getReceivedTime());
            personalChatMessageEntity.setStatus(personalChatMessageDto.getStatus());
        }

        // Common fields for both new and updated messages
        personalChatMessageEntity.setSession(
                personalSessionRepo.findById(personalChatMessageDto.getSessionid())
                        .orElseThrow(() -> new RuntimeException("Session not found"))
        );
        personalChatMessageEntity.setSender(
                userRepository.findById(personalChatMessageDto.getSenderid())
                        .orElseThrow(() -> new RuntimeException("Sender not found"))
        );
        personalChatMessageEntity.setReceiver(
                userRepository.findById(personalChatMessageDto.getReceiverid())
                        .orElseThrow(() -> new RuntimeException("Receiver not found"))
        );
        personalChatMessageEntity.setMessage(personalChatMessageDto.getMessage());

        try {
            // Save the entity (JPA will decide if it's an insert or update)
            PersonalChatMessage savedMessage = personalChatMessageRepo.save(personalChatMessageEntity);
            return ResponseEntity.ok(savedMessage);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to save message: " + e.getMessage());
        }
    }

    @Transactional
    public GroupChatMessage sendGroupMessage(GroupChatMessageDto groupChatMessageDto) {
        User sender = userRepository.findById(groupChatMessageDto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Group group = groupRepo.findById(groupChatMessageDto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        GroupChatMessage groupChatMessageEntity = new GroupChatMessage();
        groupChatMessageEntity.setGroup(group);
        groupChatMessageEntity.setMessage(groupChatMessageDto.getMessage());
        groupChatMessageEntity.setSender(sender);
        groupChatMessageEntity.setSendTime(Instant.now());

        // ✅ Create and set message statuses before saving the message
        List<GroupChatStatusMessage> messageStatuses = new ArrayList<>();

        group.getUsers().stream()
                .filter(receiver -> !Objects.equals(receiver.getId(), groupChatMessageDto.getSenderId()))
                .forEach(receiver -> {
                    GroupChatStatusMessage status = new GroupChatStatusMessage();
                    status.setMessage(groupChatMessageEntity);
                    status.setReceiver(receiver);
                    status.setGroup(group);

                    WebSocketSession receiverSession = users.get(String.valueOf(receiver.getId()));
                    if (receiverSession != null && receiverSession.isOpen()) {
                        status.setStatus(MessageStatus.RECEIVED);
                        status.setReceivedTime(Instant.now());
                    } else {
                        status.setStatus(MessageStatus.SENT);
                    }

                    messageStatuses.add(status);
                });

        // ✅ Set the message statuses to avoid orphan deletion issue
        groupChatMessageEntity.setMessageStatus(messageStatuses);

        // ✅ Save the message along with statuses
        GroupChatMessage savedMessage = groupChatMessageRepository.save(groupChatMessageEntity);
        groupChatStatusMessageRepository.saveAll(messageStatuses);

        // ✅ Notify group members via WebSocket
        group.getUsers().forEach(member -> {
            WebSocketSession memberSession = users.get(String.valueOf(member.getId()));

            if (memberSession != null && memberSession.isOpen()) {
                try {
                    // ✅ Retrieve message and modify status list instead of replacing it
                    GroupChatMessage groupMessage = groupChatMessageRepository.findById(savedMessage.getId())
                            .orElseThrow(() -> new RuntimeException("Message not found"));

                    List<GroupChatStatusMessage> statuses = groupChatStatusMessageRepository
                            .findByGroup_IdAndMessage_id(group.getId(), savedMessage.getId());

                    groupMessage.getMessageStatus().clear();  // Clear the existing list
                    groupMessage.getMessageStatus().addAll(statuses); // Add new statuses

                    memberSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            Map.of("type", "group_chat_message", "body", groupMessage))));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        return savedMessage;
    }

    @Transactional
    public void sendUpdatedGroupMessageToSender(Long messageId, Long userId) throws IOException {
        WebSocketSession senderSession = ChatHandler.users.get(String.valueOf(userId));

        if (senderSession != null && senderSession.isOpen()) {
            // Fetch the message and unwrap Optional
            GroupChatMessage updatedMessage = groupChatMessageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message not found"));

            // Convert to JSON string
            String jsonMessage = objectMapper.writeValueAsString(Map.of(
                    "type", "group_chat_message",
                    "subtype","user_received_status",
                    "body", updatedMessage
            ));

            // Send message as TextMessage
            senderSession.sendMessage(new TextMessage(jsonMessage));
        }
    }

    @Transactional
    public void groupChatMessageStatusUpdateWhenReceiverOnline(Long messageid,Long receiverid) throws IOException {
        GroupChatStatusMessage groupMessage = groupChatStatusMessageRepository.findByMessageIdAndReceiverId(messageid,receiverid);
        groupMessage.setStatus(MessageStatus.READ);
        groupMessage.setReadTime(Instant.now());
        groupChatStatusMessageRepository.save(groupMessage);

        try{
            this.sendUpdatedGroupMessageToSender(groupMessage.getMessage().getId(),groupMessage.getMessage().getSender().getId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
