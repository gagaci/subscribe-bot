package com.company.subscribebot;

import com.company.subscribebot.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Getter
public class BotDependencies {

  private final UserService userService;

}
