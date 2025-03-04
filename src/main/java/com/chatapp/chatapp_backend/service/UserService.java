package com.chatapp.chatapp_backend.service;

import com.chatapp.chatapp_backend.Enum.ChatType;
import com.chatapp.chatapp_backend.Enum.MessageStatus;
import com.chatapp.chatapp_backend.dto.CurrentUserResponseDto;
import com.chatapp.chatapp_backend.entity.PersonalChatMessage;
import com.chatapp.chatapp_backend.entity.PersonalChatSession;
import com.chatapp.chatapp_backend.entity.User;
import com.chatapp.chatapp_backend.repo.GroupChatMessageRepository;
import com.chatapp.chatapp_backend.repo.GroupChatStatusMessageRepository;
import com.chatapp.chatapp_backend.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonalChatService personalChatService;
    private final GroupChatMessageRepository groupChatMessageRepository;
    private final GroupChatStatusMessageRepository groupChatStatusMessageRepository;

    //    users list with latest message
    public List<CurrentUserResponseDto> getAllUser(Long id) {
        List<User> userEntityList = userRepository.findAll();

        List<CurrentUserResponseDto> userList = userEntityList.stream().map(user -> {
            Optional<PersonalChatSession> sessionOpt = personalChatService.findPersonalChatSession(id, user.getId());

            Long sessionId = sessionOpt.map(PersonalChatSession::getId).orElse(null);

            PersonalChatMessage latestMessage = (sessionId != null)
                    ? personalChatService.findLatestPersonalChat(sessionId)
                    : null;

            Long count = personalChatService.getUnreadMessageCount(id,user.getId());
            // Create DTO object with user data and latest message
            return new CurrentUserResponseDto(user.getId(), user.getUsername(), latestMessage,null,sessionId,count,null,null, ChatType.PRIVATE);
        }).collect(Collectors.toList());  // Collect the stream into a List first

        Optional<User> userEntity = userRepository.findById(id);
        List<CurrentUserResponseDto> groupEntityList = userEntity.get().getGroups().stream().map(group->
                new CurrentUserResponseDto(null,null,null,groupChatMessageRepository.findTopByGroup_IdOrderBySendTimeDesc(group.getId()),null,groupChatStatusMessageRepository.countByGroup_IdAndReceiver_IdAndStatusNot(group.getId(),id, MessageStatus.READ),group.getId(),group.getGroupname(),ChatType.GROUP)
        ).toList();

        List<CurrentUserResponseDto> mergedList = new ArrayList<>(userList);

        mergedList.addAll(groupEntityList);

        mergedList.sort(Comparator.comparing(
                (CurrentUserResponseDto user) -> user.getRecentMessage() != null
                        ? user.getRecentMessage().getSendTime()
                        : user.getRecentGroupMessage() != null ? user.getRecentGroupMessage().getSendTime() : Instant.MIN
        ).reversed());

        return mergedList;
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void updatePassword(String username, String newPassword) {
        User user = getUserByUsername(username);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(String username) {
        User user = getUserByUsername(username);
        userRepository.delete(user);
    }
}
