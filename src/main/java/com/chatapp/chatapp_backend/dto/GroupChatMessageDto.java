package com.chatapp.chatapp_backend.dto;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GroupChatMessageDto {
    private Long messageId;
    private String message;
    private Long senderId;
    private Long groupId;
    private MessageStatus status;
}