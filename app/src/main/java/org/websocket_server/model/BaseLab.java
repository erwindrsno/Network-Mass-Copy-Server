package org.websocket_server.model;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;

public class BaseLab implements Serializable {
  ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();
  int clientsCounts;

  public BaseLab(int labId, int clientsCountPerLab) {
    this.clientsCounts = clientsCountPerLab;
    for (int i = 1; i <= clientsCountPerLab; i++) {
      StringBuilder ip = new StringBuilder("10.100.7" + labId + ".2");
      StringBuilder hostname = new StringBuilder("LAB0" + labId + "-");
      if (i < 10) {
        ip.append("0").append(i);
        hostname.append("0").append(i);
      } else {
        ip.append(i);
        hostname.append(i);
      }
      String strIp = ip.toString();
      String strHostname = hostname.toString();
      this.clients.put(strIp, new Client(strHostname));
    }
  }

  public int getClientsCounts() {
    return clientsCounts;
  }

  public String getHostnameByIP(String ip) {
    return this.clients.get(ip).getHostname();
  }

  public boolean getStatusByIP(String ip) {
    return this.clients.get(ip).getStatus();
  }

  public void setActiveByIP(String ip) {
    this.clients.get(ip).setActive();
  }

  public void setClientConnection(String ip, WebSocket conn) {
    this.clients.get(ip).setConn(conn);
  }

  public void closeClientConnection(String ip) {
    this.clients.get(ip).closeConn();
  }

  public ConcurrentHashMap<String, Client> getClients() {
    return this.clients;
  }

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
}
