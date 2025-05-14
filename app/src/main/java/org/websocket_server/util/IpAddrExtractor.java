package org.websocket_server.util;

public class IpAddrExtractor {
  public static final Extractor IP_EXTRACTOR = conn -> conn.getRemoteSocketAddress().getAddress().getHostAddress();
}
