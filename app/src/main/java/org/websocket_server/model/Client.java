package org.websocket_server.model;

import org.java_websocket.WebSocket;

public class Client {
  private String hostname;
  private boolean isActive;
  private volatile WebSocket conn;

  public Client(String hostname) {
    this.hostname = hostname;
    this.isActive = false;
    this.conn = null;
  }

  public String getHostname() {
    return this.hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public boolean getStatus() {
    return this.isActive;
  }

  public void setActive() {
    this.isActive = true;
  }

  public void setNotActive() {
    this.isActive = false;
  }

  public WebSocket getConn() {
    return this.conn;
  }

  public void setConn(WebSocket conn) {
    this.conn = conn;
  }

  public void closeConn() {
    this.conn = null;
  }
}
