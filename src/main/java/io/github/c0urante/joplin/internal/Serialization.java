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

import io.github.c0urante.joplin.Light;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class Serialization {

  public static byte[] serializeStreamCommand(
      byte colorSpace,
      byte[] entertainmentArea,
      Light[] lights
  ) {
    ByteBuffer result = ByteBuffer.allocate(52 + 7 * lights.length);

    // Protocol name
    result.put("HueStream".getBytes(StandardCharsets.UTF_8));

    // Streaming API version (1 byte major, 1 byte minor)
    result.put((byte) 0x02);
    result.put((byte) 0x00);

    // Sequence number (1 byte, currently unused)
    result.put((byte) 0x00);

    // Reserved (2 bytes, all zeros should be sent)
    result.put((byte) 0x00);
    result.put((byte) 0x00);

    // Color space
    result.put(colorSpace);

    // Reserved (1 byte, all zeros should be sent)
    result.put((byte) 0x00);

    // Entertainment area ID
    result.put(entertainmentArea);

    // Lights (channel + color)
    for (Light light : lights) {
      light.serializeTo(result);
    }

    return result.array();
  }

}
