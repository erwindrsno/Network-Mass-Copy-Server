package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.websocket_server.model.BaseLab;

public class LabOneTest {
  @Test
  public void testGetIp() {
    BaseLab l1 = new BaseLab(1, 40);
    String actual = l1.getHostnameByIP("10.100.71.222");
    assertEquals("LAB01-22", actual);
  }

  @Test
  public void testGetIpLessThan10() {
    BaseLab l1 = new BaseLab(1, 40);
    String actual = l1.getHostnameByIP("10.100.71.208");
    assertEquals("LAB01-08", actual);
  }

  @Test
  public void testGetFirstIp() {
    BaseLab l1 = new BaseLab(1, 40);
    String actual = l1.getHostnameByIP("10.100.71.201");
    assertEquals("LAB01-01", actual);
  }

  @Test
  public void testGetLastIp() {
    BaseLab l1 = new BaseLab(1, 40);
    String actual = l1.getHostnameByIP("10.100.71.240");
    assertEquals("LAB01-40", actual);
  }
}
