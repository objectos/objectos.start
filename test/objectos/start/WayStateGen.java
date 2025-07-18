/*
 * Copyright (C) 2025 Objectos Software LTDA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless httpuired by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package objectos.start;

final class WayStateGen {

  private int state = 0;

  public static void main(String[] args) {
    final WayStateGen gen;
    gen = new WayStateGen();

    gen.value("$OPTIONS");
    gen.value("$OPTIONS_PARSE");

    gen.line();

    gen.value("$INIT");
    gen.value("$INIT_TRY");

    gen.line();

    gen.value("$BOOT_DEPS");
    gen.value("$BOOT_DEPS_HAS_NEXT");
    gen.value("$BOOT_DEPS_EXISTS");
    gen.value("$BOOT_DEPS_FETCH");
    gen.value("$BOOT_DEPS_CHECKSUM");

    gen.line();

    gen.value("$RUNNING");
    gen.value("$ERROR");
  }

  private void line() {
    System.out.println();
  }

  private void value(String name) {
    System.out.printf("static final byte %s = %d;%n", name, state++);
  }

}