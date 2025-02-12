package com.company.subscribebot.repository;

import com.company.subscribebot.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

  boolean existsByChatId(Long chatId);

}
