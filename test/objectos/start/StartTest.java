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
import java.nio.file.Path;
import java.util.Map;
import objectos.start.app.Routes;
import objectos.start.app.Y;
import objectos.way.App;
import objectos.way.Http;
import org.testng.TestNG;

public final class StartTest extends Start {

  private StartTest(Map<String, Object> bootOptions) {
    super(bootOptions);
  }

  public static void main(String[] args) {
    TestNG.main(new String[] {"-d", "work/test-output", "test/testng.xml"});
  }

  public static void startSuite(Path basedir) {
    final Map<String, Object> bootOptions;
    bootOptions = Map.of(
        "--stage", "test",
        "--basedir", basedir,
        "--class-output", Path.of("work", "main"),
        "--workdir", basedir.resolve(".objectos"),
        "logger", System.out
    );

    try (StartTest start = new StartTest(bootOptions)) {
      start.start(new String[] {});
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  final void injectorStage(App.Injector.Options ctx) {
    ctx.putInstance(STYLES_SCAN_DIRECTORY, bootOption("--class-output"));
  }

  @Override
  final Http.Handler serverHandler(App.Injector injector) {
    final Http.Routing.Module module;
    module = new Routes(injector);

    return Http.Handler.of(module);
  }

  @Override
  final Closeable server(App.Injector injector) {
    Y.INJECTOR = injector;

    Y.HANDLER = serverHandler(injector);

    // noop
    return () -> {};
  }

}