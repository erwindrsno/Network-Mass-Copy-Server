package org.websocket_server.util;

import org.java_websocket.WebSocket;

public interface ConnectionHolder {
  void setConnection(WebSocket conn, Integer port);

  WebSocket getConnection();

  Integer getPortNumber();
}
