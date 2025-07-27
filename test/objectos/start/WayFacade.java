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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import objectos.start.app.Y;

public final class WayFacade {

  // ##################################################################
  // # BEGIN: Way.Meta
  // ##################################################################

  public static final class Meta {

    final String h2Sha1 = "4fcc05d966ccdb2812ae8b9a718f69226c0cf4e2"; // sed:H2_SHA1

    final String h2Version = "2.3.232"; // sed:H2_VERSION

    final String startSha1 = "6a72bc1e83e82c5fa9c9199a0303b1bb31678839"; // sed:START_SHA1

    final String startVersion = "0.1.0-SNAPSHOT"; // sed:START_VERSION

    final String waySha1 = "733728007861811ee00cb5da51b7e898749845cb"; // sed:WAY_SHA1

    final String wayVersion = "0.2.6-SNAPSHOT"; // sed:WAY_VERSION

  }

  public static final Meta META = new Meta();

  // ##################################################################
  // # END: Way.Meta
  // ##################################################################

  // ##################################################################
  // # BEGIN: Way.Logger
  // ##################################################################

  private static final class WayLogger implements Appendable {

    final List<String> logs = new ArrayList<>();

    private WayLogger() {}

    @Override
    public final Appendable append(CharSequence csq) {
      System.out.append(csq);

      // strip out trailing newline
      String msg = csq.toString();

      msg = msg.substring(0, msg.length() - 1);

      logs.add(msg);

      return this;
    }

    @Override
    public final Appendable append(CharSequence csq, int start, int end) throws IOException {
      throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public final Appendable append(char c) throws IOException {
      throw new UnsupportedOperationException("Implement me");
    }

    @Override
    public final String toString() {
      return String.join("", logs);
    }

  }

  // ##################################################################
  // # END: Way.Logger
  // ##################################################################

  private final WayLogger logger;

  private final Way way;

  private WayFacade(WayLogger logger, Way way) {
    this.logger = logger;
    this.way = way;
  }

  public static WayFacade create() {
    final WayLogger logger;
    logger = new WayLogger();

    final Way way;
    way = new Way();

    way.clock(Y.clockIncMillis(2025, 1, 1));

    way.logger(logger);

    return new WayFacade(logger, way);
  }

  public static Closeable start(String[] args) {
    final Way way;
    way = new Way();

    return way.start(args);
  }

  public final void args(String... args) {
    way.object0(args.clone());
  }

  public final void execute(byte from, byte to) {
    way.execute(from, to);
  }

  public final String logContaining(String substring) {
    for (String log : logger.logs) {
      if (log.contains(substring)) {
        return log;
      }
    }

    throw new NoSuchElementException(substring);
  }

}