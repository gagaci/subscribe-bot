package com.company.subscribebot;

import com.company.subscribebot.service.SubscribeBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

@SpringBootApplication
public class SubscribeBotApplication {

  public static void main(String[] args) {
    String botToken = "7762894601:AAGI3IVKy9RD9_TkLMwVKTJw_H0WkX9dvME";
    try (TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication()) {
      botsApplication.registerBot(botToken, new SubscribeBot(botToken));
      System.out.println("Subscribe successfully started!");
      Thread.currentThread().join();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  }
