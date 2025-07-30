package org.websocket_server.handler;

import org.java_websocket.WebSocket;
import org.websocket_server.Server;

abstract class ConnectionHolder {
  private Server server;

  abstract void setConnection(WebSocket conn, Integer port);

  abstract WebSocket getConnection();

  abstract Integer getPortNumber();

  public void setServer(Server server) {
    this.server = server;
  }

  public Server getServer() {
    return this.server;
  }
}
