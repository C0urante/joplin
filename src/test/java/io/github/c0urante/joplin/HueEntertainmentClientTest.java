/*
 * Copyright © 2023 Chris Egerton (fearthecellos@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.c0urante.joplin;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

public class HueEntertainmentClientTest {

  private static final String TEST_CONFIG_FILE = "lights.properties";

  private static final HueColor WHITE = new Rgb(Color.WHITE);
  private static final HueColor GRAY = new Rgb(Color.GRAY);
  private static final HueColor GREEN = new Rgb(Color.GREEN);
  private static final HueColor BLUE = new Rgb(Color.BLUE);

  @Test
  public void testRapidAlternation() throws Exception {
    testAlternation(50);
  }

  @Test
  public void testAlternation() throws Exception {
    testAlternation(100);
  }

  private void testAlternation(long sleepMs) throws Exception {
    try (HueEntertainmentClient client = createClient()) {
      client.initializeStream();
      for (int i = 0; i <= 100; i++) {
        int v = i / 2;
        HueColor light1 = v % 2 == 0 ? GREEN : BLUE;
        HueColor light2 = v % 2 == 0 ? BLUE : GREEN;
        client.sendColors(light1, light2);
        Thread.sleep(sleepMs);
      }
    }
  }

  @Test
  public void testWhite() throws Exception {
    try (HueEntertainmentClient client = createClient()) {
      client.initializeStream();
      client.sendColor(2, WHITE);
      // TODO: Add API to wait for all in-flight messages to be sent by client
      Thread.sleep(1000);
    }
  }

  @Test
  @Tag("interactive")
  public void testLatency() throws Exception {
    try (HueEntertainmentClient client = createClient()) {
      client.initializeStream();

      Thread.sleep(1000);
      System.out.println("Setting both lights to gray");
      client.sendColors(GRAY, GRAY);

      for (int i = 3; i > 0; i--) {
        System.out.printf("\rSetting both lights to white in %d...", i);
        Thread.sleep(1000);
      }
      System.out.println();

      System.out.println("Setting both lights to white");
      client.sendColors(WHITE, WHITE);
      Thread.sleep(1000);

      System.out.print("Press enter to set both lights back to gray... ");
      try (Scanner stdin = new Scanner(System.in)) {
        stdin.nextLine();
      }
      client.sendColors(GRAY, GRAY);
      Thread.sleep(1000);
    }
  }

  @Test
  public void testSpecificLight() throws Exception {
    try (HueEntertainmentClient client = createClient()) {
      client.initializeStream();

      client.sendLights(new Light(0, WHITE));
      Thread.sleep(1000);
      client.sendLights(new Light(1, WHITE));
      Thread.sleep(1000);
      client.sendLights(new Light(0, GRAY));
      Thread.sleep(1000);
      client.sendLights(new Light(1, GRAY));
      Thread.sleep(1000);
    }
  }

  private static HueEntertainmentClient createClient() throws IOException {
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    try (InputStream lightsFile = classloader.getResourceAsStream(TEST_CONFIG_FILE)) {
      Properties lightsProperties = new Properties();
      lightsProperties.load(lightsFile);

      return HueEntertainmentClient.builder()
          .host(requiredProperty(lightsProperties, "bridge.host"))
          .port(bridgePort(lightsProperties))
          .username(requiredProperty(lightsProperties, "username"))
          .clientKey(requiredProperty(lightsProperties, "client.key"))
          .entertainmentArea(requiredProperty(lightsProperties, "entertainment.area.id"))
          .build();
    }
  }

  private static String requiredProperty(Properties properties, String property) {
    String result = properties.getProperty(property);
    if (result == null) {
      throw new IllegalStateException(
          "Required property " + property + " is missing. Please fill out the "
              + "src/test/java/resources/" + TEST_CONFIG_FILE
              + " file before running tests"
      );
    }
    return result;
  }

  private static int bridgePort(Properties properties) {
    String portString = properties.getProperty("bridge.port");
    if (portString == null) {
      return 2100;
    }
    return Integer.parseInt(portString);
  }

}
