package com.kakao.work.web;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.kakao.work.message.SocketMessage;
import com.kakao.work.properties.WebSocketConfigurationProperties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;


/**
 * socketController에 명시된 api 테스트
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)  // 테스트 포트 충돌 방지
public class SocketControllerTest {
  // 컨트롤러
  @Autowired
  private SocketController controller;
  
  @Autowired
  private WebSocketConfigurationProperties websocketConfigurationProperties;

  // 로컬 포트 호출
  @LocalServerPort
  private int port;
  // // ws 포트 용 url
  private String URL;

  private MockMvc mockMvc;

  private CompletableFuture<SocketMessage> completableFuture;
  private StompSession stompSession;

  // 테스트 실행전 
  @Before
  public void setup() throws Exception {
    InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
    viewResolver.setPrefix("/jsp/");
    viewResolver.setSuffix(".jsp");

    this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
      .setViewResolvers(viewResolver)
      .build();

    this.URL = "ws://localhost:" 
      + port 
      + websocketConfigurationProperties.getEndpoint().get("sockjs");

    this.completableFuture = new CompletableFuture<SocketMessage>();
    this.stompSession = createStompSession();
  }

  // 컨트롤러 로드 테스트
  @Test
  public void contexLoads() throws Exception {
    assertNotNull(controller);
  }

  // index 페이지 get 테스트
  @Test
  public void getIndexPage() throws Exception {
    this.mockMvc.perform(get("/index"))
      .andExpect(status().isOk());
  }

  // /api/chatroom API get 테스트
  @Test
  public void getChatroomList() throws Exception {
    this.mockMvc.perform(get("/api/chatroom"))
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
      .andDo(print());
  }

  // 채팅방 최초 접속 테스트
  @Test
  public void connectWebsocket() throws Exception {
    Map<String, String> prefix = websocketConfigurationProperties.getPrefix();
  
    // test1 > chatroomId 값
    Subscription subscription = this.stompSession.subscribe(prefix.get("broker") + "/connect/test1", new CreateStompFrameHandler());
    // StompHeaders stompHeaders = subscription.getSubscriptionHeaders();
    // System.out.println("stompHeaders.getDestination() : " + stompHeaders.getDestination());
    // System.out.println("stompHeaders.getLogin() : " + stompHeaders.getLogin());
    // System.out.println("stompHeaders.getSubscription() : " + stompHeaders.getSubscription());
    // System.out.println("stompHeaders.getAck() : " + stompHeaders.getAck());
    // System.out.println("stompHeaders.getSession() : " + stompHeaders.getSession());
    
    this.stompSession.send(prefix.get("destination") + "/connect/test1", new SocketMessage("test"));
    
    SocketMessage socketMessage = completableFuture.get(3, SECONDS);
    assertNotNull(socketMessage);
  }

  // 채팅방 메세지 전송 테스트
  @Test
  public void pushWebsocketMessage() throws Exception {
    Map<String, String> prefix = websocketConfigurationProperties.getPrefix();
    
    // test1 > chatroomId 값
    this.stompSession.subscribe(prefix.get("broker") + "/push/test1", new CreateStompFrameHandler());
    this.stompSession.send(prefix.get("destination") + "/push/test1", new SocketMessage("test", "text", "TestMessage"));

    SocketMessage socketMessage = completableFuture.get(3, SECONDS);
    assertNotNull(socketMessage);
  }

  @After
  public void destroy() throws Exception {
    this.stompSession.disconnect();
  }

  // websocket stomp 세션 생성
  private StompSession createStompSession() throws Exception {
    WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());

    // 연결 유지 시간 (초)
    return stompClient.connect(this.URL, new CreateStompSessionHandler()).get(5, SECONDS);
  }

  // 연결 할 웹소켓 클라이언트 리스트 생성
  private List<Transport> createTransportClient() {
    List<Transport> transports = new ArrayList<>(1);
    transports.add(new WebSocketTransport(new StandardWebSocketClient()));
    return transports;
  }

  private class CreateStompSessionHandler extends StompSessionHandlerAdapter {
    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
      System.out.println("afterConnected");
      super.afterConnected(session, connectedHeaders);
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
        Throwable exception) {
      System.out.println("handleException");
      exception.printStackTrace();
      super.handleException(session, command, headers, payload, exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
      System.out.println("handleTransportError");
      exception.printStackTrace();
      super.handleTransportError(session, exception);
    }
  }

  private class CreateStompFrameHandler implements StompFrameHandler {

    // handleFrame object 유형 결정
    @Override
    public Type getPayloadType(StompHeaders stompHeaders) {
      return SocketMessage.class;
    }

    // 
    @Override
    public void handleFrame(StompHeaders stompHeaders, Object o) {
      System.out.println("Msg " + o.toString());
      completableFuture.complete((SocketMessage) o);
    }
  }
}