package com.company.subscribebot;

import com.company.subscribebot.service.channel.ChannelService;
import com.company.subscribebot.service.group.GroupService;
import com.company.subscribebot.service.user.UserService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Getter
public class BotDependencies {

  private final UserService userService;

  private final GroupService groupService;

  private final ChannelService channelService;

}
