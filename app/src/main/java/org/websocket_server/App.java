package org.websocket_server;

import org.websocket_server.handler.WebClientHandler;
import org.websocket_server.handler.WebServerHandler;
import org.websocket_server.handler.WebSocketClientHandler;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.github.cdimascio.dotenv.Dotenv;

public class App {
  public static void main(String[] args) {
    Injector injector = Guice.createInjector(new WebSocketModule());
    WebServerHandler webServerHandler = injector.getInstance(WebServerHandler.class);
    WebClientHandler webClientHandler = injector.getInstance(WebClientHandler.class);
    WebSocketClientHandler webSocketClientHandler = injector.getInstance(WebSocketClientHandler.class);
    Dotenv dotenv = injector.getInstance(Dotenv.class);
    Server server = injector.getInstance(Server.class);

    server.injectDependencies(webServerHandler, webClientHandler, webSocketClientHandler, dotenv);
    server.run();
  }
}
