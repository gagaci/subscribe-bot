package com.company.subscribebot.service.user;

import org.telegram.telegrambots.meta.api.objects.User;

public interface UserService {

  void createUser(User tgUser, long chatId);

}
