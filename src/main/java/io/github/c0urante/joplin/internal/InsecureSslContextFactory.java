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
package io.github.c0urante.joplin.internal;

import io.github.c0urante.joplin.errors.HttpsException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class InsecureSslContextFactory {

  public static SSLContext context() {
    TrustManager trustManager = new InsecureTrustManager();

    SSLContext result;
    try {
      result = SSLContext.getInstance("TLS");
    } catch (NoSuchAlgorithmException e) {
      throw new HttpsException("Failed to create SSL context", e);
    }

    try {
      result.init(
          null,
          new TrustManager[]{trustManager},
          new SecureRandom()
      );
    } catch (KeyManagementException e) {
      throw new HttpsException("Failed to initialize SSL context", e);
    }

    return result;
  }

  private static class InsecureTrustManager extends X509ExtendedTrustManager {
    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
    }
  }

}
