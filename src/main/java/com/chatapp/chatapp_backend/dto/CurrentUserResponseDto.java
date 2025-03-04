package com.chatapp.chatapp_backend.dto;

import com.chatapp.chatapp_backend.Enum.ChatType;
import com.chatapp.chatapp_backend.entity.GroupChatMessage;
import com.chatapp.chatapp_backend.entity.PersonalChatMessage;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
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
