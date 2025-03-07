package com.chatapp.chatapp_backend.dto;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupChatMessageDto {
    private Long messageId;
    private String message;
    private Long senderId;
    private Long groupId;
    private MessageStatus status;
}