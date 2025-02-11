package com.company.subscribebot.service;

import com.company.subscribebot.entity.User;
import com.company.subscribebot.entity.UserStatus;
import com.company.subscribebot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Component
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;


  @Override
  public void createUser(org.telegram.telegrambots.meta.api.objects.User tgUser, long chatId) {

    if (userRepository.existsByTelegramId(tgUser.getId())) {
      System.out.println("the user already exists");
    } else {
      User user = User.builder()
          .telegramId(tgUser.getId())
          .firstName(tgUser.getFirstName())
          .lastName(tgUser.getLastName())
          .userName(tgUser.getUserName())
          .status(UserStatus.ACTIVE)
          .build();
      userRepository.save(user);
    }
  }

}
