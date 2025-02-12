package com.company.subscribebot.service;

import com.company.subscribebot.BotDependencies;
import com.company.subscribebot.service.channel.ChannelService;
import com.company.subscribebot.service.group.GroupService;
import com.company.subscribebot.service.user.UserService;
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
        new BotCommand("/help", "How to use the bot")
    );

    SetMyCommands setMyCommands = new SetMyCommands(commands, new BotCommandScopeDefault(), null);
    try {
      telegramClient.execute(setMyCommands);
    } catch (TelegramApiException e) {
      throw new RuntimeException(e);
    }
  }

  @SneakyThrows
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


  private void handleGroupMessage(Message message, long chatId)
      throws TelegramApiException {
    List<String> channels = List.of();
    log.info("Group message {}, with ID {}", message.getText(), chatId);

    String messageText = message.getText();

    GroupService groupService = botDependencies.getGroupService();

    groupService.createGroup(message);

    if (messageText.startsWith("/setchannel")) {
      log.info("/setchannel userId {}, chatId {}", message.getFrom().getId(), chatId);
      channels = extractChannels(messageText);

      boolean allExist = doChannelsExist(channels);

      if (!allExist) {
        sendMessage(chatId,
            """
                ❌ <b>Invalid Channel IDs</b>
                
                The provided channel IDs contain invalid or non-existent links. Please ensure that:
                ✅ The channels exist.
                ✅ The bot is an administrator in those channels.
                ✅ You have provided the correct channel usernames or IDs.
                
                Try again with the correct details.""");
        return;
      }

      ChannelService channelService = botDependencies.getChannelService();

      channelService.createChannel(channels);

    } else if (messageText.startsWith("/help")) {
      log.info("/help userId {}, chatId {}", message.getFrom().getId(), chatId);
      helpMessage(chatId);
    } else if (message.getText().startsWith("/start")) {
      welcomeMessage(chatId, message.getChat().getFirstName());
    }

    String userName = message.getFrom().getUserName();
    String firstName = message.getFrom().getFirstName();
    Long userId = message.getFrom().getId();

    List<String> memberStatuses = checkIsUserMemberInChannel(channels, userId, chatId);

    for (String memberStatus : memberStatuses) {
      log.info("User status: {}", memberStatus);

      log.info("username {}", userName);

      if (isAdmin(chatId, userId)) {
        log.info("User is an admin cannot delete message {} {}", chatId, userId);
      } else if (memberStatus.equals("left")) {
        log.info("Deleted message of userId {}, chatId {}, messageId {}", message.getFrom().getId(),
            chatId, message.getMessageId());
        deleteMessage(message, chatId);
        sendSubscribeMessage(userName, firstName, chatId, channels);
      }

    }
  }

  private void sendSubscribeMessage(String userName, String firstName, long chatId,
      List<String> channels) {
    List<String> finalChannels = eliminateAtSymbol(channels);
    String resultChannels = String.join(" ", finalChannels);
    String result = String.format(
        """
            🔔 <b>Attention!</b>
            
            Dear %s, to continue participating in this group, you must subscribe to %s.
            
            ✅ Please make sure to join the required channels and try again.""",
        (userName == null ? firstName : "@" + userName), "t.me/" + resultChannels);
    sendMessage(chatId, result);
  }

  private List<String> eliminateAtSymbol(List<String> channels) {
    return channels.stream()
        .map(channel -> channel.startsWith("@") ? channel.substring(1) : channel)
        .toList();
  }


  private List<String> checkIsUserMemberInChannel(List<String> channelIds, long userId,
      long chatId) throws TelegramApiException {
    List<String> memberStatuses = new ArrayList<>();
    for (String channelId : channelIds) {
      GetChatMember getChatMember = new GetChatMember(channelId, userId);
      ChatMember chatMember;
      try {
        chatMember = telegramClient.execute(getChatMember);
      } catch (TelegramApiException e) {
        sendMessage(chatId, "I do not have access to your <b>" + channelId + "</b>");
        throw new TelegramApiException(e);
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
      welcomeMessage(chatId, tgUser.getFirstName());
      userService.createUser(tgUser, chatId);
    } else if (message.getText().equals("/help")) {
      helpMessage(chatId);
    }
  }

  private void welcomeMessage(long chatId, String firstName) {
    String welcomeMessage = String.format(
        """
            🎉 <b>Welcome %s\
            
            </b>🚀 This bot will help you gain more subscribers to your channels swiftly. Use /help command to get instructions of the bot""",
        firstName == null ? "" : firstName);

    sendMessage(chatId, welcomeMessage);
  }

  private void helpMessage(long chatId) {
    String message = """
        🤖 <b>Welcome to the Channel Subscription Bot!</b>
        
        This bot helps guide your group members to the required channels.
        
        📌 <b>How to Set Up:</b>
        1️⃣ <b>Add the bot</b> as an admin to all required channels.
        2️⃣ <b>Use the command:</b> <code>/setchannel</code> to specify the channels.
        3️⃣ <b>Include '@' before each channel name.</b>
        4️⃣ <b>Separate channel names with spaces.</b>
        5️⃣ <b>Example format:</b> <code>/setchannel @channel1 @channel2</code>
        
        ✅ Once set up, the bot will ensure group members are subscribed before participating.""";
    sendMessage(chatId, message);
  }

  private boolean isAdmin(Long chatId, Long userId) {
    GetChatMember getChatMember = new GetChatMember(chatId.toString(), userId);
    try {
      ChatMember chatMember = telegramClient.execute(getChatMember);

      return chatMember.getStatus().equals("administrator") || chatMember.getStatus()
          .equals("creator");
    } catch (TelegramApiException e) {
      throw new RuntimeException(e);
    }
  }

}
