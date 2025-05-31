package org.websocket_server.handler;

import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;

public interface MessageHandlerStrategy {
  void handleString(String message);

  void handleByte(ByteBuffer buffer);
}
