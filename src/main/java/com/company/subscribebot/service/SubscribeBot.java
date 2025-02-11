package com.company.subscribebot.service;

import com.company.subscribebot.BotDependencies;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
public class SubscribeBot implements LongPollingSingleThreadUpdateConsumer {

  private final TelegramClient telegramClient;
  private final BotDependencies botDependencies;

  public SubscribeBot(@Value("${bot.token}") String botToken, BotDependencies botDependencies) {
    telegramClient = new OkHttpTelegramClient(botToken);
    this.botDependencies = botDependencies;
    setBotCommands();
  }

  private void setBotCommands() {
    List<BotCommand> commands = List.of(
        new BotCommand("/start", "Start the bot"),
        new BotCommand("/setchannel", "Set channels to the bot"),
        new BotCommand("/format", "Get proper format of sending channels")
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
    List<String> channels = List.of();
    log.info("Group message {}, with ID {}", message.getText(), chatId);

    String messageText = message.getText();
    if (messageText.startsWith("/setchannel")) {

      channels = extractChannels(messageText);

      boolean allExist = doChannelsExist(channels);

      if (allExist) {
        sendMessage(chatId, "📢 <b>Your channels are all valid</b>\n");

      } else {
        sendMessage(chatId, "Channel is invalid ");
      }

    } else if (messageText.startsWith("/format")) {
      showFormat(chatId);
    }

    String userName = message.getFrom().getUserName();
    String firstName = message.getFrom().getFirstName();
    Long userId = message.getFrom().getId();

    List<String> memberStatuses = checkIsUserMemberInChannel(channels, userId, chatId);

    for (String memberStatus : memberStatuses) {
      log.info("User status: {}", memberStatus);

      log.info("username {}", userName);

      if (memberStatus.equals("left")) {
        deleteMessage(message, chatId);

        String stringBuilder = "<b> Please </b> "
            + (userName == null ? firstName : '@' + userName)
            + " subscribe to ";

        sendMessage(chatId, stringBuilder);
      }

    }
  }


  private List<String> checkIsUserMemberInChannel(List<String> channelIds, long userId,
      long chatId) {
    List<String> memberStatuses = new ArrayList<>();
    for (String channelId : channelIds) {
      GetChatMember getChatMember = new GetChatMember(channelId, userId);
      ChatMember chatMember;
      try {
        chatMember = telegramClient.execute(getChatMember);
      } catch (TelegramApiException e) {
        sendMessage(chatId, "I do not have access to your <b>" + channelId + "</b>");
        throw new RuntimeException(e);
      }
      memberStatuses.add(chatMember.getStatus());
    }
    return memberStatuses;
  }

  private void deleteMessage(Message message, long chatId) {
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


  private void showFormat(long chatId) {
    String format = "/setChannel @channelId1 @channelId2";
    sendMessage(chatId, format);
  }

  @SneakyThrows
  public void sendMessage(Long chatId, String text) {
    SendMessage message = SendMessage
        .builder()
        .chatId(chatId)
        .text(text)
        .parseMode("HTML")
        .build();

    message.enableHtml(true);
    telegramClient.execute(message);
  }


  private boolean doChannelsExist(List<String> channels) {
    for (String channel : channels) {
      GetChat getChat = new GetChat(channel);
      try {
        Chat chat = telegramClient.execute(getChat);
        if (chat == null) {
          return false;
        }
      } catch (TelegramApiException e) {
        return false;
      }
    }
    return true;
  }

  private List<String> extractChannels(String textMessage) {
    String[] parts = textMessage.split("\\s+");

    if (parts.length > 1) {
      return Arrays.asList(parts).subList(1, parts.length);
    }
    return Collections.emptyList();
  }

  private void handlePrivateMessage(Message message, long chatId) {
    log.info("private message {}, with ID {}", message.getText(), chatId);
    if (message.getText().equals("/start")) {
      UserService userService = botDependencies.getUserService();
      User tgUser = message.getFrom();
      userService.createUser(tgUser, chatId);
      String stringBuilder = "Welcome "
          + message.getFrom().getFirstName()
          + " to subscribe bot 🌐";

      sendMessage(chatId, stringBuilder);
    }
  }
}
