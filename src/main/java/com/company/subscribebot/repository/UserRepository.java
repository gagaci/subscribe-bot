package com.company.subscribebot.repository;

import com.company.subscribebot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByTelegramId(Long telegramId);

}
