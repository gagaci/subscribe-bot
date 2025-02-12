package com.company.subscribebot.service.group;

import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface GroupService {

  void createGroup(Message message);

}
