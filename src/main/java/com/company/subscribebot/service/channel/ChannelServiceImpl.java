package com.company.subscribebot.service.channel;

import com.company.subscribebot.entity.Channel;
import com.company.subscribebot.repository.ChannelRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ChannelServiceImpl implements ChannelService {

  private final ChannelRepository channelRepository;

  @Override
  public void createChannel(List<String> channelIds) {
    for (String channelName : channelIds) {
      if (!channelRepository.existsByChannelName(channelName)) {
        Channel channel = Channel
            .builder().channelName(channelName)
            .build();
        channelRepository.save(channel);
      }
    }
  }
}
