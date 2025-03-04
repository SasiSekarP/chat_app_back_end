package com.chatapp.chatapp_backend.controller;

import com.chatapp.chatapp_backend.dto.CurrentUserResponseDto;
import com.chatapp.chatapp_backend.entity.User;
import com.chatapp.chatapp_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // Get user details
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponseDto> getUserDetails(Authentication authentication) {
        String username = authentication.getName();
        User userEntity = userService.getUserByUsername(username);
        CurrentUserResponseDto currentUserResponseDto = new CurrentUserResponseDto();
        currentUserResponseDto.setId(userEntity.getId());
        currentUserResponseDto.setUsername(userEntity.getUsername());
        return ResponseEntity.ok(currentUserResponseDto);
    }

    @GetMapping("/users")
    public  ResponseEntity<List<CurrentUserResponseDto>> getAllUsers(@RequestParam Long id){
        List<CurrentUserResponseDto> userList = userService.getAllUser(id).stream().filter(user ->
                user.getId() != id
                ).toList();
        if (userList.isEmpty()) {
            return ResponseEntity.noContent().build(); // Returns 204 No Content
        }
        return ResponseEntity.ok(userList);
    }

    // Update user password
    @PutMapping("/update-password")
    public ResponseEntity<String> updatePassword(Authentication authentication, @RequestBody String newPassword) {
        String username = authentication.getName();
        userService.updatePassword(username, newPassword);
        return ResponseEntity.ok("Password updated successfully!");
    }

    // Delete user account
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount(Authentication authentication) {
        String username = authentication.getName();
        userService.deleteUser(username);
        return ResponseEntity.ok("User deleted successfully!");
    }
}

