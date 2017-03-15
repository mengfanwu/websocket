package chat;

import help.HTMLFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
/**
 * Created by mengfw on 2017/3/15.
 */
@ServerEndpoint(value = "/websocket/chat")
public class Chat {
  private static final Logger LOGGER = LoggerFactory.getLogger(Chat.class);

  private static final String GUEST_PREFIX = "Guest";
  private static final AtomicInteger connectionIds = new AtomicInteger(0);
  private static final Map<String,Object> connections = new HashMap<String,Object>();

  private final String nickname;
  private Session session;

  public Chat() {
    nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
  }
  static {
    LOGGER.info("class loaded");
  }
  {
    LOGGER.info("instance created");
  }
  @OnOpen
  public void start(Session session) {
    this.session = session;
    connections.put(nickname, this);
    String message = String.format("* %s %s", nickname, "has joined.");
    broadcast(message);
  }


  @OnClose
  public void end() {
    connections.remove(this);
    String message = String.format("* %s %s",
        nickname, "has disconnected.");
    broadcast(message);
  }


  /**
   * 消息发送触发方法
   * @param message
   */
  @OnMessage
  public void incoming(String message) {
    // Never trust the client
    String filteredMessage = String.format("%s: %s",
        nickname, HTMLFilter.filter(message.toString()));
    broadcast(filteredMessage);
  }

  @OnError
  public void onError(Throwable t) throws Throwable {
    LOGGER.error("Chat Error: " + t.toString(), t);
  }

  /**
   * 消息发送方法
   * @param msg
   */
  private static void broadcast(String msg) {
//    if(msg.indexOf("Guest0")!=-1){
//      sendUser(msg);
//    } else{
//      sendAll(msg);
//    }
    sendAll(msg);
  }

  /**
   * 向所有用户发送
   * @param msg
   */
  public static void sendAll(String msg){
    for (String key : connections.keySet()) {
      Chat client = null ;
      try {
        client = (Chat) connections.get(key);
        synchronized (client) {
          if(client.session.isOpen()){
            client.session.getBasicRemote().sendText(msg);
          }else{
            LOGGER.error("session is closed key ={}",key);
          }
        }
      } catch (IOException e) {
        LOGGER.debug("Chat Error: Failed to send message to client", e);
        connections.remove(client);
        try {
          client.session.close();
        } catch (IOException e1) {
          // Ignore
        }
        String message = String.format("* %s %s",
            client.nickname, "has been disconnected.");
        broadcast(message);
      }
    }
  }

  /**
   * 向指定用户发送消息
   * @param msg
   */
  public static void sendUser(String msg){
    Chat c = (Chat)connections.get("Guest0");
    try {
      c.session.getBasicRemote().sendText(msg);
    } catch (IOException e) {
      LOGGER.debug("Chat Error: Failed to send message to client", e);
      connections.remove(c);
      try {
        c.session.close();
      } catch (IOException e1) {
        // Ignore
      }
      String message = String.format("* %s %s",
          c.nickname, "has been disconnected.");
      broadcast(message);
    }
  }
}
