package com.chatapp.chatapp_backend.service;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import com.chatapp.chatapp_backend.entity.PersonalChatSession;
import com.chatapp.chatapp_backend.entity.User;
import com.chatapp.chatapp_backend.repo.GroupChatStatusMessageRepository;
import com.chatapp.chatapp_backend.repo.PersonalChatSessionRepo;
import com.chatapp.chatapp_backend.repo.UserRepository;
import com.chatapp.chatapp_backend.dto.UserDto;
import com.chatapp.chatapp_backend.security.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final UserService userService;
    private final PersonalChatService personalChatService;
    private final PersonalChatSessionRepo personalChatSessionRepo;
    private final GroupChatStatusMessageRepository groupChatStatusMessageRepository;

    @PostConstruct
    public void createDefaultUsers() {
        User user1 = createUserIfNotExists("User1", "User1@123");
        User user2 = createUserIfNotExists("User2", "User2@123");
        User user3 = createUserIfNotExists("User3", "User3@123");

        // Create chat sessions between default users
        createChatSessionIfNotExists(user1, user2);
        createChatSessionIfNotExists(user1, user3);
        createChatSessionIfNotExists(user2, user3);
    }

    private User createUserIfNotExists(String username, String password) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
            return user;
        });
    }

    private void createChatSessionIfNotExists(User user1, User user2) {
        if (personalChatService.findPersonalChatSession(user1.getId(), user2.getId()).isEmpty()) {
            PersonalChatSession chatSession = new PersonalChatSession();
            chatSession.setUser1(user1);
            chatSession.setUser2(user2);
            personalChatSessionRepo.save(chatSession);
        }
    }

    public Map<String,String> register(UserDto request) {
        Optional<User> existingUser = userRepository.findByUsername(request.getUsername());
        if (existingUser.isPresent()) {
            throw new RuntimeException("Username already exists!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User currentUser =userRepository.save(user);

        List<User> userEntityList = userRepository.findAll();

        if(!userEntityList.isEmpty()){
            userEntityList.stream()
                    .filter(eachUser -> eachUser.getId() != currentUser.getId()) // Avoid self-chat session
                    .forEach(eachUser -> {
                        Optional<PersonalChatSession> existingSession = personalChatService.findPersonalChatSession(eachUser.getId(), currentUser.getId());
                        if (existingSession.isEmpty()) {
                            PersonalChatSession newSession = new PersonalChatSession();
                            newSession.setUser1(eachUser);
                            newSession.setUser2(currentUser);
                            personalChatSessionRepo.save(newSession);
                        }
                    });

        }

        return Map.of("message","User registered successfully! Login using login page");
    }

    public Map<String, String> login(UserDto request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        List<PersonalChatSession> personalChatSessionEntityList = personalChatSessionRepo.findAllByUserId(user.getId());

        personalChatSessionEntityList.forEach(session ->
                personalChatService.changeMassageStatus(session.getId(), user.getId(), MessageStatus.SENT,MessageStatus.RECEIVED)
        );

        user.getGroups().stream().forEach(group-> {
                    groupChatStatusMessageRepository.findByReceiver_idAndStatus(user.getId(),MessageStatus.SENT).forEach(groupMessageStatus ->
                            {
                                groupMessageStatus.setStatus(MessageStatus.RECEIVED);
                                groupMessageStatus.setReceivedTime(Instant.now());
                                groupChatStatusMessageRepository.save(groupMessageStatus);

                                try {
                                    personalChatService.sendUpdatedGroupMessageToSender(groupMessageStatus.getMessage().getId(),groupMessageStatus.getMessage().getSender().getId());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );
                }
                );

        return jwtUtil.generateToken(user.getUsername());
    }

    public void forgotPassword(UserDto userDto){
        User user = userService.getUserByUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        userRepository.save(user);
    }
}