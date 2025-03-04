package com.chatapp.chatapp_backend.dto;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import com.chatapp.chatapp_backend.entity.PersonalChatSession;
import com.chatapp.chatapp_backend.entity.User;
import lombok.*;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RecentMessageDto {
    private Long id;
    private PersonalChatSession session;
    private User sender;
    private User receiver;
    private String message;
    private MessageStatus status;
    private Instant sendTime;
    private Instant receivedTime;
    private Instant readTime;
}
