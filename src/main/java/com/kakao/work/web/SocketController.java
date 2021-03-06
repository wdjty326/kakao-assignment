package com.kakao.work.web;

import java.util.List;
import java.util.Map;

import com.kakao.work.message.SocketMessage;
import com.kakao.work.properties.ChatRoomConfigurationProperties;
import com.kakao.work.properties.WebSocketConfigurationProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 메세지 제어 소스
 */
@Controller
public class SocketController {
  // 로거
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Autowired
  private ChatRoomConfigurationProperties chatroomConfigurationProperties;

  @Autowired
  private WebSocketConfigurationProperties websocketConfigurationProperties;
  /**
   * index.jsp 호출
   */
  @GetMapping(value={"/index", "/chat/**"})
  public String index(Model model) {
    return "index";
  }
  
  @GetMapping("/api/chatroom")
  public @ResponseBody List<Map<String, String>> chatroom() {
    List<Map<String, String>> list = chatroomConfigurationProperties.getList();
    this.logger.info("@/api/chatroom@response@" + list);
    return list;
  }

  /**
   * 최초 사용자 접속 메세지 전달
   */
  @MessageMapping("/connect/{chatroomId}")
  @SendTo("/topic/connect/{chatroomId}")
  public SocketMessage connect(@DestinationVariable String chatroomId, @Payload SocketMessage message) throws Exception {
    Thread.sleep(200); // 딜레이 부여
    logger.info("@connect@message@" + message);
    message.setType("connect");
    return message;
  }

  /**
   * 메세지 전송
   */
  @MessageMapping("/push/{chatroomId}")
  @SendTo("/topic/push/{chatroomId}")
  public SocketMessage push(@DestinationVariable String chatroomId, @Payload SocketMessage message) throws Exception {
    Thread.sleep(200); // 딜레이 부여
    logger.info("@push@message@" + message);
    message.setType("push");
    return message;
  }
}