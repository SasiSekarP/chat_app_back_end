package com.chatapp.chatapp_backend.controller;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import com.chatapp.chatapp_backend.dto.GroupDto;
import com.chatapp.chatapp_backend.entity.GroupChatMessage;
import com.chatapp.chatapp_backend.entity.GroupChatStatusMessage;
import com.chatapp.chatapp_backend.entity.Group;
import com.chatapp.chatapp_backend.entity.User;
import com.chatapp.chatapp_backend.repo.GroupChatMessageRepository;
import com.chatapp.chatapp_backend.repo.GroupChatStatusMessageRepository;
import com.chatapp.chatapp_backend.repo.GroupRepository;
import com.chatapp.chatapp_backend.repo.UserRepository;
import com.chatapp.chatapp_backend.service.GroupService;
import com.chatapp.chatapp_backend.service.PersonalChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group")
public class GroupController {

    private final GroupService groupService;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupChatMessageRepository groupChatMessageRepository;
    private final GroupChatStatusMessageRepository groupChatStatusMessageRepository;
    private final PersonalChatService personalChatService;

    @PostMapping
    public Group createGroup (@RequestBody GroupDto groupDTO) throws JsonProcessingException {
        return groupService.createGroup(groupDTO);
    }

    @PutMapping("/{groupid}")
    public ResponseEntity<?> updateGroup(@PathVariable Long groupid, @RequestBody GroupDto groupDTO) {
        System.out.println("update group hit");

        Group groupEntity = new Group();
        groupEntity.setId(groupid);
        groupEntity.setGroupname(groupDTO.getGroupname());

        List<User> users = groupDTO.getUsers().stream()
                .map(userId -> userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId)))
                .toList();

        List<User> admins = groupDTO.getAdmins().stream()
                .map(userId -> userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + userId)))
                .toList();

        groupEntity.setUsers(users);
        groupEntity.setAdmins(admins);

        try {
            return groupService.updateGroup(groupEntity);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while updating group: " + e.getMessage());
        }
    }

    @DeleteMapping("/{groupid}")
    public ResponseEntity<?> deleteGroup (@PathVariable Long id){
        try {
            if(!groupRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found!");
            }
            groupRepository.deleteById(id);
            return ResponseEntity.ok("Group Deleted successfully");

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while delete group: " + e.getMessage());
        }
    }

    @GetMapping("/{groupid}")
    public ResponseEntity<?> getSingleGroup(@PathVariable Long groupid){
        try{
            if(groupRepository.existsById(groupid)){
                return ResponseEntity.ok(groupRepository.findById(groupid));
            }else{
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found!");
            }
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("group-message/{groupid}/{userid}")
    public List<GroupChatMessage> getGroupMessages(@PathVariable Long groupid, @PathVariable Long userid){
        List<GroupChatStatusMessage> groupChatStatusMessageEntities =groupChatStatusMessageRepository.findByReceiver_idAndStatus(userid, MessageStatus.RECEIVED);
        groupChatStatusMessageEntities.stream().forEach(groupChatStatusMessageEntity -> {
            groupChatStatusMessageEntity.setStatus(MessageStatus.READ);
            groupChatStatusMessageEntity.setReadTime(Instant.now());
            groupChatStatusMessageRepository.save(groupChatStatusMessageEntity);

            try {
                personalChatService.sendUpdatedGroupMessageToSender(groupChatStatusMessageEntity.getMessage().getId(),groupChatStatusMessageEntity.getMessage().getSender().getId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return groupChatMessageRepository.findByGroupIdAndUserId(groupid,userid);
    }
}
