package org.websocket_server;

import java.net.InetSocketAddress;

import org.websocket_server.handler.WebClientHandler;
import org.websocket_server.handler.WebServerHandler;
import org.websocket_server.handler.WebSocketClientHandler;
import org.websocket_server.util.FileVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import io.github.cdimascio.dotenv.Dotenv;

public class WebSocketModule extends AbstractModule {
  @Override
  protected void configure() {
    Dotenv dotenv = Dotenv.configure().load();
    bind(Dotenv.class).toInstance(dotenv); // make Dotenv available everywhere

    bind(String.class).annotatedWith(Names.named("host")).toInstance(dotenv.get("LOCAL_WEBSOCKET_IP"));
    bind(Integer.class).annotatedWith(Names.named("port")).toInstance(Integer.parseInt(dotenv.get("PORT")));
    bind(FileVerifier.class).in(Singleton.class);
    bind(WebServerHandler.class).in(Singleton.class);
    bind(WebClientHandler.class).in(Singleton.class);
    bind(WebSocketClientHandler.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  public Logger provideLogger() {
    Logger logger = LoggerFactory.getLogger(WebSocketModule.class);
    return logger;
  }

  @Provides
  @Singleton
  public Server provideServer(Logger logger, @Named("host") String host, @Named("port") int port) {
    try {
      return new Server(new InetSocketAddress(host, port));
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  // @Provides
  // @Singleton
  // public WebSocketServer provideWebSocketServer(Server providedServer, Logger
  // logger) {
  // try {
  // WebSocketServer server = providedServer;
  // return server;
  // } catch (Exception e) {
  // logger.error(e.getMessage(), e);
  // return null;
  // }
  // }
}
