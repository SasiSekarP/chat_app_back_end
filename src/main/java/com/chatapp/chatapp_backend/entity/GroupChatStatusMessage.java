package com.chatapp.chatapp_backend.entity;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name="group_chat_status")
public class GroupChatStatusMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "message_id",nullable = false)
    @JsonBackReference
    private GroupChatMessage message;

    @ManyToOne
    @JoinColumn(name="receiver_id")
    private User receiver;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @JsonSerialize(using = InstantSerializer.class)
    private Instant receivedTime;

    @JsonSerialize(using = InstantSerializer.class)
    private Instant readTime;

}
