package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.websocket_server.model.BaseLab;

public class LabTwoTest {
  @Test
  public void testGetIp() {
    BaseLab l2 = new BaseLab(2, 45);
    String actual = l2.getHostnameByIP("10.100.72.215");
    assertEquals("LAB02-15", actual);
  }

  @Test
  public void testGetIpLessThan10() {
    BaseLab l2 = new BaseLab(2, 45);
    String actual = l2.getHostnameByIP("10.100.72.205");
    assertEquals("LAB02-05", actual);
  }

  @Test
  public void testGetFirstIp() {
    BaseLab l2 = new BaseLab(2, 45);
    String actual = l2.getHostnameByIP("10.100.72.201");
    assertEquals("LAB02-01", actual);
  }

  @Test
  public void testGetLastIp() {
    BaseLab l2 = new BaseLab(2, 45);
    String actual = l2.getHostnameByIP("10.100.72.245");
    assertEquals("LAB02-45", actual);
  }
}
