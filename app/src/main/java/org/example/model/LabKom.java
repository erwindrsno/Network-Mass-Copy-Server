package org.example.model;

import java.util.concurrent.ConcurrentHashMap;

public class LabKom extends Lab {
  private final ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();

  private int clientsCounts;

  public LabKom(int labId, int clientsCountPerLab) {
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

  @Override
  public ConcurrentHashMap<String, Client> getClients() {
    return this.clients;
  }

  public int getClientsCounts() {
    return this.clientsCounts;
  }
}
