package org.websocket_server.util;

import org.java_websocket.WebSocket;

@FunctionalInterface
public interface Extractor {
  String extract(WebSocket conn);
}
