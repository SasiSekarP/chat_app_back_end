package com.chatapp.chatapp_backend.dto;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import lombok.*;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
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