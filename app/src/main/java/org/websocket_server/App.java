package org.websocket_server;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class App {
  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new WebSocketModule());
    Server server = injector.getInstance(Server.class);
    server.run();
  }
}
