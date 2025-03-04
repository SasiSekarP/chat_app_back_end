package com.chatapp.chatapp_backend.entity;

import com.chatapp.chatapp_backend.Enum.MessageStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "personal_chat_message")
public class PersonalChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "session_id")
    @ManyToOne
    private PersonalChatSession session;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    private String message;

    @Enumerated(EnumType.STRING)
    private MessageStatus status;

    @CreationTimestamp
    @JsonSerialize(using = InstantSerializer.class)
    private Instant sendTime;

    @Column(nullable = true)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant receivedTime;

    @Column(nullable = true)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant readTime;
}
