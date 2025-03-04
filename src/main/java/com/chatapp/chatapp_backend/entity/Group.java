package com.chatapp.chatapp_backend.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "groups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupname;

    @ManyToMany
    @JoinTable(
            name = "group_users", // Create a junction table to represent the relationship
            joinColumns = @JoinColumn(name = "group_id"), // Foreign key to GroupEntity
            inverseJoinColumns = @JoinColumn(name = "user_id") // Foreign key to UserEntity
    )
    @JsonManagedReference("user-groups")
    private List<User> users;

    @ManyToMany
    @JsonManagedReference("admin-groups")
    @JoinTable(
            name = "admin_users",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> admins;
}

