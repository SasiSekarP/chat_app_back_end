package com.chatapp.chatapp_backend.controller;

import com.chatapp.chatapp_backend.entity.PersonalChatSession;
import com.chatapp.chatapp_backend.service.PersonalChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/personal-chat")
@RequiredArgsConstructor
public class PersonalChatController {

    private final PersonalChatService personalChatService;

    @GetMapping
    public ResponseEntity<?> findPersonalChat(@RequestParam Long senderid, @RequestParam Long receiverid){
        Optional<PersonalChatSession> personalChatSessionEntity =personalChatService.findPersonalChatSession(senderid,receiverid);
        if(!personalChatSessionEntity.isEmpty()){
            return personalChatService.findPersonalChats(personalChatSessionEntity.get().getId(),senderid);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Session not fount");
    }
}
