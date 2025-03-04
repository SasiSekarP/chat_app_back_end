package com.chatapp.chatapp_backend.dto;

import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GroupDto {
    private String groupname;
    private List<Long> users;
    private List<Long> admins;
}
