/*
 * Copyright (C) 2025 Objectos Software LTDA.
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
import objectos.start.Y.Project;
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

  private static final String DEV_CLASS_OUTPUT = Path.of("work", "main").toAbsolutePath().toString();

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
        "--basedir", basedir.toString(),
        "--dev-class-output", DEV_CLASS_OUTPUT,
        "--workdir", basedir.resolve(".objectos").toString(),
        "--repo-boot", basedir.resolve(Path.of(".objectos", "boot")).toString(),
        "--repo-remote", Y.repoRemoteArg(),
        "--port", Integer.toString(port)
    );
  }

  @Override
  public final void startWith(String... args) {
    final Way way;
    way = new Way();

    server = way.start(args);

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

    final Page page;
    page = browser.newPage();

    return new YTab(baseUrl, page);
  }

}
