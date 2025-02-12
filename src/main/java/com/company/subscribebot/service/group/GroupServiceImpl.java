package com.company.subscribebot.service.group;

import com.company.subscribebot.entity.Group;
import com.company.subscribebot.repository.GroupRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@AllArgsConstructor
public class GroupServiceImpl implements GroupService {

  private final GroupRepository groupRepository;

  @Override
  public void createGroup(Message message) {
    if (!groupRepository.existsByChatId(message.getChatId())) {
      Group group = Group.builder()
          .groupName(message.getChat().getTitle())
          .chatId(message.getChatId())
          .build();
      groupRepository.save(group);
    }
  }
}
