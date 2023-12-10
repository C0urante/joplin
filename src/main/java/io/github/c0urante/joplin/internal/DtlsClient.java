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

import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.DTLSClientProtocol;
import org.bouncycastle.tls.DTLSTransport;
import org.bouncycastle.tls.DatagramTransport;
import org.bouncycastle.tls.PSKTlsClient;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.TlsPSKIdentity;
import org.bouncycastle.tls.TlsUtils;
import org.bouncycastle.tls.UDPTransport;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DtlsClient implements AutoCloseable {

  private final DTLSTransport transport;

  public DtlsClient(String hostnameOrIpAddress, int port, TlsPSKIdentity pskIdentity) throws IOException {
    BouncyCastleClient bouncyCastleClient = new BouncyCastleClient(pskIdentity);

    InetAddress address = InetAddress.getByName(hostnameOrIpAddress);
    DatagramSocket socket = new DatagramSocket();
    // Extremely conservative. From the docs:
    //   "After 10 seconds of no activity the connection is closed automatically"
    socket.setSoTimeout(30_000);
    socket.connect(address, port);

    int mtu = 1500;
    DatagramTransport transport = new UDPTransport(socket, mtu);

    DTLSClientProtocol protocol = new DTLSClientProtocol();

    this.transport = protocol.connect(bouncyCastleClient, transport);
  }

  public void send(byte[] message) throws IOException {
    transport.send(message, 0, message.length);
  }

  @Override
  public void close() throws IOException {
    transport.close();
  }

  private static class BouncyCastleClient extends PSKTlsClient {

    public BouncyCastleClient(TlsPSKIdentity pskIdentity) {
      super(new BcTlsCrypto(), pskIdentity);
    }

    @Override
    protected ProtocolVersion[] getSupportedVersions() {
      return ProtocolVersion.DTLSv12.only();
    }

    @Override
    protected int[] getSupportedCipherSuites() {
      return TlsUtils.getSupportedCipherSuites(
          getCrypto(),
          new int[]{CipherSuite.TLS_PSK_WITH_AES_128_GCM_SHA256}
      );
    }
  }

}
