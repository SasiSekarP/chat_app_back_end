package com.chatapp.chatapp_backend.service;

import com.chatapp.chatapp_backend.Enum.ChatType;
import com.chatapp.chatapp_backend.dto.CurrentUserResponseDto;
import com.chatapp.chatapp_backend.dto.GroupDto;
import com.chatapp.chatapp_backend.entity.GroupChatSession;
import com.chatapp.chatapp_backend.entity.Group;
import com.chatapp.chatapp_backend.entity.User;
import com.chatapp.chatapp_backend.repo.GroupChatSessionRepo;
import com.chatapp.chatapp_backend.repo.GroupRepository;
import com.chatapp.chatapp_backend.repo.UserRepository;
import com.chatapp.chatapp_backend.websocket.ChatHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GroupChatSessionRepo groupChatSessionRepo;

    public Group createGroup(GroupDto groupDTO) throws JsonProcessingException {
        List<User> groupUsers = groupDTO.getUsers().stream()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        List<User> admins = groupDTO.getAdmins().stream()
                .map(userRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        Group group = new Group();
        group.setGroupname(groupDTO.getGroupname());
        group.setUsers(groupUsers);
        group.setAdmins(admins);

        Group createdGroup = groupRepository.save(group);

        GroupChatSession groupChatSessionEntity = new GroupChatSession();
        groupChatSessionEntity.setGroup(createdGroup);
        GroupChatSession groupSession = groupChatSessionRepo.save(groupChatSessionEntity);

        String createdGroupJson = objectMapper.writeValueAsString(new CurrentUserResponseDto(null,null,null,null,groupSession.getId(),null,createdGroup.getId(),createdGroup.getGroupname(), ChatType.GROUP));

        groupUsers.forEach(groupUser -> {
            WebSocketSession receiverSession = ChatHandler.users.get(String.valueOf(groupUser.getId()));
            if(receiverSession != null && receiverSession.isOpen()){
                try {
                    receiverSession.sendMessage(new TextMessage(createdGroupJson));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        return createdGroup;
    }

    @Transactional
    public ResponseEntity<?> updateGroup(Group groupEntity){
        try {
            Group oldGroupDetails = groupRepository.getById(groupEntity.getId());

            //            group name update
            if(!oldGroupDetails.getGroupname().equals(groupEntity.getGroupname())){
                oldGroupDetails.getUsers().forEach(groupUser -> {
                    WebSocketSession receiverSession = ChatHandler.users.get(String.valueOf(groupUser.getId()));
                    String createdGroupJson = null;
                    try {
                        createdGroupJson = objectMapper.writeValueAsString(Map.of("chatType","UPDATED_GROUP_NAME","id",groupEntity.getId(),"groupName", groupEntity.getGroupname()));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    if(receiverSession != null && receiverSession.isOpen()){
                        try {
                            receiverSession.sendMessage(new TextMessage(createdGroupJson));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            // Fetch the existing admins from the database
            List<User> existingAdmins = oldGroupDetails.getAdmins();

            // Get the new admins from the request
            List<User> newAdmins = groupEntity.getAdmins();

            // Find admins that are in newAdmins but not in existingAdmins
            List<User> addedAdmins = newAdmins.stream()
                    .filter(admin -> !existingAdmins.contains(admin))
                    .collect(Collectors.toList());

            List<User> removedAdmins = existingAdmins.stream().filter(admin -> !newAdmins.contains(admin)).collect(Collectors.toList());

            List<User> existingUsers = oldGroupDetails.getUsers();

            List<User> newUsers = groupEntity.getUsers();

            List<User> addedUsers = newUsers.stream()
                    .filter(admin -> !existingUsers.contains(admin))
                    .collect(Collectors.toList());

            List<User> removedUsers = existingUsers.stream()
                    .filter(admin -> !newUsers.contains(admin))
                    .collect(Collectors.toList());

            Group updatedGroup = groupRepository.save(groupEntity);

//            update to the new users
            if(addedUsers.size() > 0){
                GroupChatSession groupSession = groupChatSessionRepo.findByGroup_Id(updatedGroup.getId());
                String createdGroupJson = objectMapper.writeValueAsString(new CurrentUserResponseDto(null,null,null,null,groupSession.getId(),null,updatedGroup.getId(),updatedGroup.getGroupname(), ChatType.GROUP));
                addedUsers.forEach(groupUser -> {
                    WebSocketSession receiverSession = ChatHandler.users.get(String.valueOf(groupUser.getId()));
                    if(receiverSession != null && receiverSession.isOpen()){
                        try {
                            receiverSession.sendMessage(new TextMessage(createdGroupJson));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

//            update to the removed users
            if(removedUsers.size()>0){
                removedUsers.forEach(groupUser -> {
                    WebSocketSession receiverSession = ChatHandler.users.get(String.valueOf(groupUser.getId()));
                    String createdGroupJson = null;
                    try {
                        createdGroupJson = objectMapper.writeValueAsString(Map.of("chatType","YOU_ARE_REMOVED","group_id",updatedGroup.getId(),"removedGroupName", updatedGroup.getGroupname()));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    if(receiverSession != null && receiverSession.isOpen()){
                        try {
                            receiverSession.sendMessage(new TextMessage(createdGroupJson));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

//            update to the new admins
            if(addedAdmins.size()>0){
                Group groupDetails = groupRepository.getById(groupEntity.getId());
                addedAdmins.forEach(groupUser -> {
                    WebSocketSession receiverSession = ChatHandler.users.get(String.valueOf(groupUser.getId()));
                    String createdGroupJson = null;
                    try {
                        createdGroupJson = objectMapper.writeValueAsString(Map.of("chatType","UPDATED_ADMIN_STATUS","Message","You are admin now...!","group_details", groupDetails));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    if(receiverSession != null && receiverSession.isOpen()){
                        try {
                            receiverSession.sendMessage(new TextMessage(createdGroupJson));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

//            update to the removed admins
            if(removedAdmins.size()>0){
                Group groupDetails = groupRepository.getById(groupEntity.getId());
                removedAdmins.forEach(groupUser -> {
                    WebSocketSession receiverSession = ChatHandler.users.get(String.valueOf(groupUser.getId()));
                    String createdGroupJson = null;
                    try {
                        createdGroupJson = objectMapper.writeValueAsString(Map.of("chatType","UPDATED_ADMIN_STATUS","Message","You are removed from admin list...!","group_details", groupDetails));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    if(receiverSession != null && receiverSession.isOpen()){
                        try {
                            receiverSession.sendMessage(new TextMessage(createdGroupJson));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            return ResponseEntity.ok(updatedGroup);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

    }

}