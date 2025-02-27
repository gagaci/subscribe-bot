package com.company.subscribebot.config;

import com.company.subscribebot.BotDependencies;
import com.company.subscribebot.service.SubscribeBot;
import com.company.subscribebot.service.channel.ChannelService;
import com.company.subscribebot.service.group.GroupService;
import com.company.subscribebot.service.user.UserService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class BotConfig {

  @Bean
  public BotDependencies botDependencies(UserService userService, GroupService groupService,
      ChannelService channelService, MessageSource messageSource) {
    return new BotDependencies(userService, groupService, channelService, messageSource);
  }

  @Bean
  public SubscribeBot subscribeBot(@Value("${bot.token}") String botToken,
      BotDependencies botDependencies) {
    return new SubscribeBot(botToken, botDependencies);
  }
}
