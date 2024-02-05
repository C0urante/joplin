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

import java.nio.charset.StandardCharsets;

public class Validation {

  public static byte[] entertainmentArea(
      String entertainmentArea
  ) {
    byte[] result = entertainmentArea.getBytes(
        StandardCharsets.UTF_8
    );

    if (result.length != 36)
      throw new IllegalArgumentException(
          "Invalid value " + entertainmentArea
              + " for entertainment area ID; "
              + "must be exactly 36 bytes long"
      );

    return result;
  }

  public static void red(int red) {
    validateColor(red, "red");
  }

  public static void green(int green) {
    validateColor(green, "green");
  }

  public static void blue(int blue) {
    validateColor(blue, "blue");
  }

  private static void validateColor(int value, String color) {
    if (value > 0xFFFF)
      throw new IllegalArgumentException(
          "Invalid value " + value
              + " for color " + color
              + "; must be between 0 and 65535, inclusive"
      );
  }

  public static int tries(int value) {
    if (value <= 0) {
      throw new IllegalArgumentException(
          "Invalid value " + value
              + " for tries; "
              + "must positive"
      );
    }
    return value;
  }

  public static byte colorSpace(int colorSpace) {
    if (colorSpace < 0 || colorSpace > 255)
      throw new IllegalArgumentException(
          "Invalid value " + colorSpace
              + " for color space; "
              + "must be between 0 and 255, inclusive"
      );

    return (byte) colorSpace;
  }

}
