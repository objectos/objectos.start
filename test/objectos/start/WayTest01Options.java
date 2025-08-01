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

import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public final class WayTest01Options {

  @Test
  public void classOutput01() {
    final WayFacade way;
    way = WayFacade.create();

    way.args("--stage", "dev", "--class-output", "work/main");

    way.execute(Way.$OPTIONS, Way.$INIT_TRY);

    final String log;
    log = way.logContaining("(CLI) --class-output");

    assertTrue(log.endsWith("work/main"));
  }

  @Test
  public void repoRemote01() {
    final WayFacade way;
    way = WayFacade.create();

    way.args("--repo-remote", "work/test-repo/");

    way.execute(Way.$OPTIONS, Way.$BOOT_DEPS);

    final String log;
    log = way.logContaining("(CLI) --repo-remote");

    assertTrue(log.endsWith("work/test-repo/"));
  }

}