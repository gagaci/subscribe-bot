package com.company.subscribebot.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
public class SubscribeBot implements LongPollingSingleThreadUpdateConsumer {

  private final TelegramClient telegramClient;

  public SubscribeBot(String botToken) {
    telegramClient = new OkHttpTelegramClient(botToken);
    setBotCommands();
  }

  private void setBotCommands() {
    List<BotCommand> commands = List.of(
        new BotCommand("/start", "Start the bot")
    );

    SetMyCommands setMyCommands = new SetMyCommands(commands, new BotCommandScopeDefault(), null);

    try {
      telegramClient.execute(setMyCommands);
    } catch (TelegramApiException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void consume(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      Message message = update.getMessage();
      Chat chat = message.getChat();
      String chatType = chat.getType();
      long chatId = update.getMessage().getChatId();

      switch (chatType) {
        case "private" -> handlePrivateMessage(message, chatId);
        case "group", "supergroup" -> handleGroupMessage(message, chatId);
      }
    }
  }

  private void handleGroupMessage(Message message, long chatId) {
    log.info("Group message {}, with ID {}", message.getText(), chatId);
    DeleteMessage deleteMessage = DeleteMessage.builder()
        .messageId(message.getMessageId())
        .chatId(chatId)
        .build();

    try {
      telegramClient.execute(deleteMessage);
    } catch (TelegramApiException e) {
      throw new RuntimeException(e);
    }
  }

  private void handlePrivateMessage(Message message, long chatId) {
    log.info("private message {}, with ID {}", message.getText(), chatId);
    if (message.getText().equals("/start")) {
      SendMessage sendMessage = SendMessage
          .builder()
          .chatId(chatId)
          .text("welcome")
          .build();
      try {
        telegramClient.execute(sendMessage);
      } catch (TelegramApiException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
