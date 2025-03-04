package com.chatapp.chatapp_backend.repo;

import com.chatapp.chatapp_backend.entity.PersonalChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalChatSessionRepo extends JpaRepository<PersonalChatSession, Long> {

    @Query("SELECT p FROM PersonalChatSession p " +
            "WHERE (p.user1.id = :userId OR p.user2.id = :userId)")
    List<PersonalChatSession> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM PersonalChatSession p WHERE (p.user1.id = :user1Id AND p.user2.id = :user2Id) OR (p.user1.id = :user2Id AND p.user2.id = :user1Id)")
    Optional<PersonalChatSession> findByUser1_IdAndUser2_Id(Long user1Id, Long user2Id);
}
