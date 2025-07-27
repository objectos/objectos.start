/*
 * Objectos Start
 * Copyright (C) 2025 Objectos Software LTDA.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

    gen.value("$LAYER");

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