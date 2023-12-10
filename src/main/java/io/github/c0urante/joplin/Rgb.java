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

import io.github.c0urante.joplin.internal.Validation;

import java.awt.Color;
import java.nio.ByteBuffer;

public class Rgb implements HueColor {

  private final int red;
  private final int green;
  private final int blue;

  public Rgb(Color color) {
    this(
        color.getRed() << 8,
        color.getGreen() << 8,
        color.getBlue() << 8
    );
  }

  public Rgb(int red, int green, int blue) {
    Validation.red(red);
    Validation.green(green);
    Validation.blue(blue);

    this.red = red;
    this.green = green;
    this.blue = blue;
  }

  @Override
  public void serializeTo(ByteBuffer byteBuffer) {
    byteBuffer.put((byte) ((red >> 8) & 0xFF));
    byteBuffer.put((byte) (red & 0xFF));
    byteBuffer.put((byte) ((green >> 8) & 0xFF));
    byteBuffer.put((byte) (green & 0xFF));
    byteBuffer.put((byte) ((blue >> 8) & 0xFF));
    byteBuffer.put((byte) (blue & 0xFF));
  }

}
