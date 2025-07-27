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
package objectos.start.app;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import objectos.start.WayFacade;
import objectos.start.app.Y.Project;
import objectos.way.Note;

final class YProject implements Y.Project {

  private record Notes(
      Note.Ref1<String> stderr,
      Note.Ref1<String> stdout,
      Note.Ref1<IOException> ioException
  ) {

    static Notes get() {
      Class<?> s;
      s = Project.class;

      return new Notes(
          Note.Ref1.create(s, "STE", Note.INFO),
          Note.Ref1.create(s, "STO", Note.INFO),
          Note.Ref1.create(s, "IOX", Note.ERROR)
      );
    }

  }

  private static final String CLASS_OUTPUT = Path.of("work", "main").toAbsolutePath().toString();

  private final Path basedir;

  private Browser browser;

  private final Notes notes = Notes.get();

  private final Note.Sink noteSink = Y.noteSink();

  private Playwright playwright;

  private final int port;

  private Closeable server;

  YProject(YProjectBuilder builder) {
    basedir = builder.basedir();

    port = builder.port;
  }

  @Override
  public final void close() {
    try {
      closeProcess();
    } finally {
      if (playwright != null) {
        playwright.close();
      }
    }
  }

  private void closeProcess() {
    if (server != null) {
      try {
        server.close();
      } catch (IOException e) {
        noteSink.send(notes.ioException, e);
      }
    }
  }

  @Override
  public final Set<String> ls() {
    try (Stream<Path> walk = Files.walk(basedir)) {
      return walk
          .filter(path -> !path.equals(basedir))
          .filter(Files::isRegularFile)
          .map(basedir::relativize)
          .map(Object::toString)
          .collect(Collectors.toUnmodifiableSet());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public final String readString(String path) {
    try {
      final Path file;
      file = resolve(path);

      return Files.readString(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public final Path resolve(String path) {
    return basedir.resolve(path);
  }

  @Override
  public final void start() {
    startWith(
        "--stage", "dev",
        "--basedir", basedir.toString(),
        "--class-output", CLASS_OUTPUT,
        "--workdir", basedir.resolve(".objectos").toString(),
        "--repo-boot", basedir.resolve(Path.of(".objectos", "boot")).toString(),
        "--repo-remote", Y.repoRemoteArg(),
        "--port", Integer.toString(port)
    );
  }

  @Override
  public final void startWith(String... args) {
    server = WayFacade.start(args);

    if (server == null) {
      throw new AssertionError("Server failed to start");
    }

    playwright = Playwright.create();

    final BrowserType chromium;
    chromium = playwright.chromium();

    final boolean headless;
    headless = Boolean.getBoolean("playwright.headless");

    final LaunchOptions launchOptions;
    launchOptions = new BrowserType.LaunchOptions().setHeadless(headless);

    browser = chromium.launch(launchOptions);
  }

  @Override
  public final Y.Tab newTab() {
    final String prefix;
    prefix = basedir.getFileName().toString();

    final String baseUrl;
    baseUrl = "http://" + prefix + ".localhost:" + port;

    Browser.NewPageOptions options;
    options = new Browser.NewPageOptions().setBaseURL(baseUrl);

    final boolean headless;
    headless = Boolean.getBoolean("playwright.headless");

    if (!headless) {
      options = options.setViewportSize(null);
    }

    final Page page;
    page = browser.newPage(options);

    return new YTab(baseUrl, page);
  }

}
