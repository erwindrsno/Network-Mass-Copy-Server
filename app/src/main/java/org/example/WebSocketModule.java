package org.example;

import java.net.InetSocketAddress;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class WebSocketModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(String.class).annotatedWith(Names.named("host")).toInstance("192.168.0.106");
    bind(Integer.class).annotatedWith(Names.named("port")).toInstance(8887);
    bind(FileVerifier.class).in(Singleton.class);
    bind(WebServerHandler.class).in(Singleton.class);
    bind(WebClientHandler.class).in(Singleton.class);
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

  @Provides
  @Singleton
  public WebSocketServer provideWebSocketServer(Server providedServer, Logger logger) {
    try {
      WebSocketServer server = providedServer;
      return server;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }
}
