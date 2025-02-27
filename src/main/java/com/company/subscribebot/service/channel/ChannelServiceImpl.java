package com.company.subscribebot.service.channel;

import com.company.subscribebot.entity.Channel;
import com.company.subscribebot.repository.ChannelRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ChannelServiceImpl implements ChannelService {

  private final ChannelRepository channelRepository;

  @Override
  public void createChannel(Channel channel) {
    if (!channelRepository.existsByChannelName(channel.getChannelName())) {
      channelRepository.save(channel);
    }
  }
}