package com.chatapp.chatapp_backend.dto;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonalChatMessageDto {

    private Long id;

    private Long sessionid;

    private Long senderid;

    private Long receiverid;

    private String message;

    private MessageStatus status;

    private Instant receivedTime;

    private Instant readTime;
}