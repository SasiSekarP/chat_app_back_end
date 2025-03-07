package com.chatapp.chatapp_backend.dto;

import com.chatapp.chatapp_backend.Enum.ChatType;
import com.chatapp.chatapp_backend.entity.GroupChatMessage;
import com.chatapp.chatapp_backend.entity.PersonalChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUserResponseDto {
    private Long id;
    private String username;
    private PersonalChatMessage recentMessage;
    private GroupChatMessage recentGroupMessage;
    private Long sessionid;
    private Long count;
    private Long groupId;
    private String groupName;
    private ChatType chatType;
}
