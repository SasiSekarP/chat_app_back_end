package com.chatapp.chatapp_backend.controller;

import com.chatapp.chatapp_backend.dto.UserDto;
import com.chatapp.chatapp_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor // Generates a constructor with required fields (final)
public class AuthController {

    private final AuthService authService; // Will be injected automatically

    @PostMapping("/register")
    public ResponseEntity<Map<String,String>> register(@RequestBody UserDto request) {
        try {
            return ResponseEntity.ok(authService.register(request));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error",e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String,String>> login(@RequestBody UserDto request) {
        try{
            return ResponseEntity.ok(authService.login(request));
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error",e.getMessage()));
        }
    }

    @PutMapping("/forgot-password")
    public  ResponseEntity<Map<String,String>> forgetPossword(@RequestBody UserDto userDto){
        try{
            authService.forgotPassword(userDto);
            return ResponseEntity.ok(Map.of("message","Password changed successfully!"));
        } catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message",e.getMessage()));
        }
    }
}
