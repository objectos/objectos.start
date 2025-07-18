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

import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public final class WayTest01Options {

  @Test
  public void repoRemote01() {
    final Y.WayTester way;
    way = Y.wayTester();

    way.args("--repo-remote", "work/test-repo/");

    way.execute(Way.$OPTIONS, Way.$BOOT_DEPS);

    final String log;
    log = way.logContaining("(CLI) --repo-remote");

    assertTrue(log.endsWith("work/test-repo/"));
  }

}