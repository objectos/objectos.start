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

import static org.testng.Assert.assertEquals;

import java.util.Set;
import objectos.start.app.Y;
import org.testng.annotations.Test;

public final class WayTest02 {

  @Test
  public void testCase01() {
    try (Y.Project proj = Y.project(opts -> {
      opts.addFile("main/module-info.java", """
      module objectos.test {
        exports objectos.test;

        requires objectos.way;
      }
      """);

      opts.addFile("main/objectos/test/Start.java", """
      package objectos.test;
      import objectos.way.App;
      public final class Start extends App.Bootstrap {
        @Override
        protected final void bootstrap() {
        }
      }
      """);
    })) {
      proj.start();

      assertEquals(proj.ls(), Set.of(
          ".objectos/boot/" + WayFacade.META.waySha1 + ".jar",
          "Way.java",
          "main/module-info.java",
          "main/objectos/test/Start.java"
      ));

      final Y.Tab tab;
      tab = proj.newTab();

      tab.navigate("/");

      assertEquals(tab.title(), "Welcome!");

      tab.dev();
    }
  }

}