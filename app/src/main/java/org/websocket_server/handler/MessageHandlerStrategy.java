package org.websocket_server.handler;

import java.nio.ByteBuffer;

public interface MessageHandlerStrategy {
  void handleString(String message);

  void handleByte(ByteBuffer buffer);
}
