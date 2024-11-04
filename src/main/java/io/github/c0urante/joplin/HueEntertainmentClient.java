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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.c0urante.joplin.internal.DtlsClient;
import io.github.c0urante.joplin.internal.InsecureSslContextFactory;
import io.github.c0urante.joplin.internal.Serialization;
import io.github.c0urante.joplin.internal.Validation;
import org.bouncycastle.tls.BasicTlsPSKIdentity;
import org.bouncycastle.tls.TlsPSKIdentity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A synchronous client for the
 * <a href="https://developers.meethue.com/develop/hue-entertainment/hue-entertainment-api/">
 *   Philips Hue Entertainment API</a>.
 * <p>
 * This client can prepare a stream for initialization
 */
public class HueEntertainmentClient implements AutoCloseable {

  private static final Duration REST_CONNECT_TIMEOUT = Duration.ofSeconds(5);

  private final TlsPSKIdentity pskIdentity;
  private final String host;
  private final int port;
  private final String username;
  private final byte colorSpace;
  private final byte[] entertainmentArea;
  private final int tries;
  private final URI baseUri;
  private final HttpClient httpClient;

  private Thread httpThread;
  private DtlsClient dtlsClient = null;

  private HueEntertainmentClient(
      String host,
      int port,
      String username,
      String clientKey,
      int colorSpace,
      String entertainmentArea,
      int tries
  ) {
    Objects.requireNonNull(host, "Host name / IP address must be set");
    Objects.requireNonNull(username, "Username must be set");
    Objects.requireNonNull(clientKey, "Client key must be set");
    Objects.requireNonNull(entertainmentArea, "Entertainment area ID must be set");

    this.pskIdentity = new BasicTlsPSKIdentity(username, parseClientKey(clientKey));
    this.host = host;
    this.port = port;
    this.username = username;
    this.colorSpace = Validation.colorSpace(colorSpace);
    this.entertainmentArea = Validation.entertainmentArea(entertainmentArea);
    this.tries = Validation.tries(tries);

    this.httpClient = HttpClient.newBuilder()
        .sslContext(InsecureSslContextFactory.context())
        // TODO: Configurable?
        .connectTimeout(REST_CONNECT_TIMEOUT)
        .build();

    String baseUriString = "https://" + host;
    try {
      this.baseUri = new URI(baseUriString);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(
          "Failed to parse URI from string '" + baseUriString + "'",
          e
      );
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link HueEntertainmentClient}.
   */
  public static class Builder {

    public static final int DEFAULT_PORT = 2100;
    public static final int DEFAULT_COLOR_SPACE = HueColor.COLOR_SPACE_RGB;
    public static final int DEFAULT_TRIES = 3;

    private String host = null;
    private int port = DEFAULT_PORT;
    private String username = null;
    private String clientKey = null;
    private int colorSpace = DEFAULT_COLOR_SPACE;
    private String entertainmentArea = null;
    private int tries = DEFAULT_TRIES;

    private Builder() {
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder clientKey(String clientKey) {
      this.clientKey = clientKey;
      return this;
    }

    // TODO
    // public Builder colorSpace(int colorSpace) {
    //   this.colorSpace = colorSpace;
    //   return this;
    // }

    public Builder entertainmentArea(String entertainmentArea) {
      this.entertainmentArea = entertainmentArea;
      return this;
    }

    public Builder tries(int tries) {
      this.tries = tries;
      return this;
    }

    public HueEntertainmentClient build() {
      return new HueEntertainmentClient(
          host,
          port,
          username,
          clientKey,
          colorSpace,
          entertainmentArea,
          tries
      );
    }

  }

  /**
   * Prepare to start streaming to the bridge. This needs to be invoked before
   * any colors can be sent via, e.g., {@link #sendColors(HueColor...)} or
   * {@link #sendLights(Light...)}.

   * @throws IOException if an error occurs while contacting the bridge's REST API
   * @throws InterruptedException if the client is closed or the calling thread is
   * otherwise interrupted while contacting the bridge's REST API
   *
   * @see <a href="https://developers.meethue.com/develop/hue-entertainment/hue-entertainment-api/#getting-started-with-streaming-api">
   *   Hue Entertainment API guide, step 5</a>
   */
  public void initializeStream() throws IOException, InterruptedException {
    synchronized (this) {
      httpThread = Thread.currentThread();
    }

    sendEntertainmentConfigurationRequest(true);

    synchronized (this) {
      httpThread = null;
    }

    if (dtlsClient != null) {
      dtlsClient.close();
      dtlsClient = null;
    }

    this.dtlsClient = new DtlsClient(
        host,
        port,
        pskIdentity
    );
  }

  /**
   * Set multiple lights to a single, fixed color.
   * @param numLights the number of lights to set (the first light will be channel 0,
   *                  the next will be channel 1, and so on)
   * @param color the color to set the lights to
   * @throws IOException if an error occurs while contacting the bridge's DTLS API
   */
  public void sendColor(int numLights, HueColor color) throws IOException {
    Light[] lights = IntStream.range(0, numLights)
        .mapToObj(i -> new Light(i, color))
        .toArray(Light[]::new);

    sendLights(lights);
  }

  /**
   * Set lights to a series of colors. The first color will be sent to channel 0, the next
   * will be sent to channel 1, and so on.
   * @param colors the colors to set the lights to
   * @throws IOException if an error occurs while contacting the bridge's DTLS API
   */
  public void sendColors(List<HueColor> colors) throws IOException {
    sendColors(colors.toArray(new HueColor[0]));
  }

  /**
   * Set lights to a series of colors. The first color will be sent to channel 0, the next
   * will be sent to channel 1, and so on.
   * @param colors the colors to set the lights to
   * @throws IOException if an error occurs while contacting the bridge's DTLS API
   */
  public void sendColors(HueColor... colors) throws IOException {
    Light[] lights = IntStream.range(0, colors.length)
        .mapToObj(i -> new Light(i, colors[i]))
        .toArray(Light[]::new);

    sendLights(lights);
  }

  /**
   * Set colors for specific lights.
   * @param lights the lights to set
   * @throws IOException if an error occurs while contacting the bridge's DTLS API
   */
  public void sendLights(List<Light> lights) throws IOException {
    sendLights(lights.toArray(new Light[0]));
  }

  /**
   * Set colors for specific lights.
   * @param lights the lights to set
   * @throws IOException if an error occurs while contacting the bridge's DTLS API
   */
  public void sendLights(Light... lights) throws IOException {
    // TODO: Consider lazily initializing the DTLS client here so that users can
    //       initialize streams via their own flow.
    if (dtlsClient == null) {
      throw new IllegalStateException("Must initialize stream before sending colors to bridge");
    }

    if (lights.length == 0) {
      return;
    }

    byte[] serialized = Serialization.serializeStreamCommand(
        colorSpace,
        entertainmentArea,
        lights
    );

    // UDP, baby
    for (int i = 0; i < tries; i++) {
      dtlsClient.send(serialized);
    }
  }

  /**
   * Close the client, releasing all underlying resources and interrupting any
   * in-progress requests.
   */
  @Override
  public void close() throws IOException, InterruptedException {
    synchronized (this) {
      if (httpThread != null) {
        httpThread.interrupt();
        httpThread = null;
        sendEntertainmentConfigurationRequest(false);
      }
    }

    if (dtlsClient != null) {
      dtlsClient.close();
      dtlsClient = null;
    }
  }

  private static byte[] parseClientKey(String clientKey) {
    if (clientKey.length() != 32) {
      throw new IllegalArgumentException("Client key must be 32 bytes long");
    }

    byte[] result = new byte[clientKey.length() / 2];

    for (int i = 0; i < clientKey.length(); i += 2) {
      result[i / 2] = (byte) (Integer.parseInt(clientKey.substring(i, i + 2), 16));
    }

    return result;
  }

  private void sendEntertainmentConfigurationRequest(boolean start) throws IOException, InterruptedException {
    ObjectMapper objectMapper = new ObjectMapper();

    Map<String, String> requestBody = new HashMap<>();
    String action = start ? "start" : "stop";
    requestBody.put("action", action);
    String serializedRequestBody = objectMapper.writeValueAsString(requestBody);

    String entertainmentAreaString = new String(
        entertainmentArea,
        StandardCharsets.UTF_8
    );

    URI requestUri = baseUri.resolve(
        "/clip/v2/resource/entertainment_configuration/" + entertainmentAreaString
    );

    HttpRequest request = HttpRequest.newBuilder(requestUri)
        .PUT(HttpRequest.BodyPublishers.ofString(serializedRequestBody))
        .header("hue-application-key", username)
        .build();

    HttpResponse<String> response = httpClient.send(
        request,
        HttpResponse.BodyHandlers.ofString()
    );

    if (response.statusCode() < 200 | response.statusCode() >= 300) {
      throw new IOException(
          "Request failed with status code " + response.statusCode()
              + "; body: " + response.body()
      );
    }

    JsonNode deserializedResponseBody = objectMapper.readTree(response.body());
    if (!deserializedResponseBody.isObject()) {
      throw new IOException(
          "Expected response to be JSON object, but was "
              + deserializedResponseBody.getNodeType()
      );
    }

    JsonNode errors = deserializedResponseBody.get("errors");
    if (errors != null && errors.isArray() && !errors.isEmpty()) {
      throw new IOException(
          "Response contains errors: " + errors
      );
    }
  }

}
