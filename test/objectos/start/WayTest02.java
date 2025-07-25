/*
 * Copyright (C) 2023-2025 Objectos Software LTDA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package objectos.start;

import static org.testng.Assert.assertEquals;

import java.util.Set;
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
          ".objectos/boot/" + Y.META.startSha1 + ".jar",
          ".objectos/boot/" + Y.META.waySha1 + ".jar",
          "Way.java",
          "main/module-info.java",
          "main/objectos/test/Start.java"
      ));

      final Y.Tab tab;
      tab = proj.newTab();

      tab.navigate("/");

      assertEquals(tab.title(), "Welcome!");
    }
  }

}