package org.example.model;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;

public abstract class Lab implements Serializable {
  abstract ConcurrentHashMap<String, Client> getClients();

  public Lab() {
  }

  public int getClientsCounts() {
    return getClients().size();
  }

  public String getHostnameByIP(String ip) {
    return getClients().get(ip).getHostname();
  }

  public boolean getStatusByIP(String ip) {
    return getClients().get(ip).getStatus();
  }

  public void setActiveByIP(String ip) {
    getClients().get(ip).setActive();
  }

  public void setClientConnection(String ip, WebSocket conn) {
    getClients().get(ip).setConn(conn);
  }

  public void closeClientConnection(String ip) {
    getClients().get(ip).closeConn();
  }

  // public ConcurrentHashMap<String, Client> getClients() {
  // return this.clients;
  // }
}
