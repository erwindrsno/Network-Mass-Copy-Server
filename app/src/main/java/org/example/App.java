package org.example;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class App {
  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new WebSocketModule());
    WebServerHandler webServerHandler = injector.getInstance(WebServerHandler.class);
    WebClientHandler webClientHandler = injector.getInstance(WebClientHandler.class);
    Server server = injector.getInstance(Server.class);

    server.injectDependencies(webServerHandler, webClientHandler);
    server.run();
  }
}
