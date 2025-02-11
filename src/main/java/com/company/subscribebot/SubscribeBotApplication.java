package com.company.subscribebot;

import com.company.subscribebot.service.SubscribeBot;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@SpringBootApplication
@AllArgsConstructor
public class SubscribeBotApplication {

  private final SubscribeBot subscribeBot;

  public static void main(String[] args) throws TelegramApiException {
    SpringApplication.run(SubscribeBotApplication.class, args).getBean(SubscribeBotApplication.class).run();
  }

  public void run() throws TelegramApiException {
    TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();

    // Register our bot
    String botToken = "7762894601:AAEqngkeXnp_l0sNcPVVGXYrqKdzWXOYdVc";
    botsApplication.registerBot(botToken, this.subscribeBot);
    System.out.println("Subscribe bot successfully started!");
  }
}