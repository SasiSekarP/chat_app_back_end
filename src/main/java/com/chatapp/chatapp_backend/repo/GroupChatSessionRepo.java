package com.chatapp.chatapp_backend.repo;

import com.chatapp.chatapp_backend.entity.GroupChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupChatSessionRepo extends JpaRepository<GroupChatSession,Long> {
    GroupChatSession findByGroup_Id(Long group_id);
}
