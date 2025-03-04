package com.chatapp.chatapp_backend.websocket;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import com.chatapp.chatapp_backend.dto.GroupChatMessageDto;
import com.chatapp.chatapp_backend.dto.PersonalChatMessageDto;
import com.chatapp.chatapp_backend.service.PersonalChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class ChatHandler extends TextWebSocketHandler {

    // Stores user ID -> WebSocketSession
    public static ConcurrentHashMap<String, WebSocketSession> users = new ConcurrentHashMap<>();

    // Stores session -> user ID (for removing on disconnect)
    private static ConcurrentHashMap<WebSocketSession, String> sessionUserMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PersonalChatService personalChatService;

    @Autowired
    public ChatHandler(PersonalChatService personalChatService) {
        this.personalChatService = personalChatService;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();

        JsonNode jsonNode = objectMapper.readTree(payload);

//      common
        String type = jsonNode.hasNonNull("type") ? jsonNode.get("type").asText() : null;
        String sender = jsonNode.hasNonNull("senderid") ? jsonNode.get("senderid").asText() : null;

//      private message
        String receiver = jsonNode.hasNonNull("receiverid") ? jsonNode.get("receiverid").asText() : null;
        String text = jsonNode.hasNonNull("message") ? jsonNode.get("message").asText() : null;
        Long sessionId = jsonNode.hasNonNull("sessionid") ? jsonNode.get("sessionid").asLong() : null;

        String id = jsonNode.hasNonNull("id") ? jsonNode.get("id").asText() : null;

//      group message
        String groupId = jsonNode.hasNonNull("groupId") ? jsonNode.get("groupId").asText() : null;

        if(groupId != null&&!groupId.isEmpty()){
            GroupChatMessageDto groupChatMessageDto = new GroupChatMessageDto();
            if(type != null && !type.isEmpty() && type.equals("CREATE")){
                groupChatMessageDto.setMessage(text);
                groupChatMessageDto.setSenderId(Long.valueOf(sender));
                groupChatMessageDto.setGroupId(Long.valueOf(groupId));
                groupChatMessageDto.setStatus(MessageStatus.SENT);
                personalChatService.sendGroupMessage(groupChatMessageDto);
            }
        }else if(type != null && !type.isEmpty() && type.equals("GROUPMESSAGEUPDATE")){
            try{
                personalChatService.groupChatMessageStatusUpdateWhenReceiverOnline(Long.valueOf(text),Long.valueOf(receiver));
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            PersonalChatMessageDto personalChatMessageDto = new PersonalChatMessageDto();

            personalChatMessageDto.setSessionid(sessionId);
            personalChatMessageDto.setMessage(text);
            personalChatMessageDto.setSenderid(Long.valueOf(sender));
            personalChatMessageDto.setReceiverid(Long.valueOf(receiver));

            // Send message only to the intended recipient
            WebSocketSession recipientSession = users.get(receiver);
            WebSocketSession SenderSession = users.get(sender);

            if (type != null && !type.isEmpty() && !type.equals("CREATE")) {
                personalChatMessageDto.setId(Long.valueOf(id));
                personalChatMessageDto.setStatus(MessageStatus.READ);
                personalChatMessageDto.setReadTime(Instant.now());
            } else {
                personalChatMessageDto.setStatus(recipientSession != null && recipientSession.isOpen() ? MessageStatus.RECEIVED : MessageStatus.SENT);
                personalChatMessageDto.setReceivedTime(recipientSession != null && recipientSession.isOpen() ? Instant.now() : null);
            }

            ResponseEntity<?> savedMessage = personalChatService.savePersonalChatMessage(personalChatMessageDto);

            String savedMessageJson = objectMapper.writeValueAsString(savedMessage);

            if (recipientSession != null && recipientSession.isOpen()) {
                recipientSession.sendMessage(new TextMessage(savedMessageJson));
            }
            SenderSession.sendMessage(new TextMessage(savedMessageJson));
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserIdFromSession(session); // Extract user ID from query params
        users.put(userId, session);
        sessionUserMap.put(session, userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionUserMap.get(session);
        if (userId != null) {
            users.remove(userId);
            sessionUserMap.remove(session);
        }
    }

    private String getUserIdFromSession(WebSocketSession session) {
        // Extract user ID from WebSocket connection URL query string (e.g., ws://localhost:8080/chat?userId=john)
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("userId=")) {
            return query.split("=")[1];
        }
        return "unknown";
    }
}