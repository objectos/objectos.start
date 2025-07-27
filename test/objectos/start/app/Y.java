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

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.function.Consumer;
import objectos.start.StartTest;
import objectos.way.App;
import objectos.way.Http;
import objectos.way.Io;
import objectos.way.Note;
import org.testng.ISuite;
import org.testng.ISuiteListener;

public final class Y implements ISuiteListener {

  public static App.Injector INJECTOR;

  public static Http.Handler HANDLER;

  /// required by TestNG ISuiteListener
  public Y() {}

  @Override
  public final void onStart(ISuite suite) {
    if (INJECTOR == null) {
      final Path basedir;
      basedir = nextTempDir();

      StartTest.startSuite(basedir);
    }
  }

  // ##################################################################
  // # BEGIN: Clock
  // ##################################################################

  private static final Clock FIXED = Clock.fixed(
      LocalDateTime.of(2025, 4, 28, 13, 1).atZone(ZoneOffset.UTC).toInstant(),
      ZoneOffset.UTC
  );

  public static Clock clockIncMillis(int year, int month, int day) {
    final LocalDateTime dateTime;
    dateTime = LocalDateTime.of(year, month, day, 10, 0);

    final ZonedDateTime startTime;
    startTime = dateTime.atZone(ZoneId.systemDefault());

    return new Clock() {
      private long milis;

      @Override
      public final Instant instant() {
        final Instant instant;
        instant = startTime.toInstant();

        return instant.plusMillis(milis++);
      }

      @Override
      public final ZoneId getZone() {
        return startTime.getZone();
      }

      @Override
      public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
      }
    };
  }

  // ##################################################################
  // # END: Clock
  // ##################################################################

  public static String handle(Http.Exchange http) {
    HANDLER.handle(http);

    final YResponseListener listener;
    listener = http.get(YResponseListener.class);

    return listener.toString();
  }

  public static Http.Exchange http(Consumer<? super Http.Exchange.Options> more) {
    final YResponseListener listener;
    listener = new YResponseListener(4);

    return Http.Exchange.create(options -> {
      options.clock(FIXED);

      options.responseListener(listener);

      options.set(YResponseListener.class, listener);

      more.accept(options);
    });
  }

  // ##################################################################
  // # BEGIN: Next
  // ##################################################################

  private static final class NextPath implements Closeable {

    private final Path root;

    private NextPath(Path root) {
      this.root = root;
    }

    private static NextPath create() {
      try {
        final Path root;
        root = Files.createTempDirectory("objectos-start-testing-");

        final NextPath nextPath;
        nextPath = new NextPath(root);

        shutdownHook(nextPath);

        return nextPath;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public final void close() throws IOException {
      Io.deleteRecursively(root);
    }

    final Path nextTempDir() {
      try {
        return Files.createTempDirectory(root, "next-temp-dir");
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    final Path nextTempFile() {
      try {
        return Files.createTempFile(root, "next-temp-file", ".tmp");
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

  }

  private static final class NextPathHolder {

    static final NextPath INSTANCE = NextPath.create();

  }

  public static Path nextTempDir() {
    return NextPathHolder.INSTANCE.nextTempDir();
  }

  public static Path nextTempFile() {
    return NextPathHolder.INSTANCE.nextTempFile();
  }

  public static Path nextTempFile(String contents) {
    return nextTempFile(contents, StandardCharsets.UTF_8);
  }

  public static Path nextTempFile(String contents, Charset charset) {
    try {
      final Path file;
      file = NextPathHolder.INSTANCE.nextTempFile();

      Files.writeString(file, contents, charset);

      return file;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  // ##################################################################
  // # END: Next
  // ##################################################################

  // ##################################################################
  // # BEGIN: Note.Sink
  // ##################################################################

  private static final App.NoteSink INSTANCE = App.NoteSink.sysout();

  public static Note.Sink noteSink() {
    return INSTANCE;
  }

  // ##################################################################
  // # END: Note.Sink
  // ##################################################################

  // ##################################################################
  // # BEGIN: Project
  // ##################################################################

  public sealed interface Project extends AutoCloseable permits YProject {

    sealed interface Options permits YProjectBuilder {

      void addFile(String relativePath, String contents);

    }

    @Override
    void close();

    // FS

    Set<String> ls();

    String readString(String path);

    Path resolve(String path);

    // process

    void start();

    void startWith(String... args);

    // browser

    Tab newTab();

  }

  public static Project project(Consumer<? super Y.Project.Options> opts) {
    final YProjectBuilder builder;
    builder = new YProjectBuilder();

    opts.accept(builder);

    return new YProject(builder);
  }

  // ##################################################################
  // # END: Project
  // ##################################################################

  // ##################################################################
  // # BEGIN: Repo
  // ##################################################################

  private static final Path REPO_REMOTE = Path.of("work/test-repo/").toAbsolutePath();

  public static String repoRemoteArg() {
    return REPO_REMOTE.toString() + "/";
  }

  // ##################################################################
  // # END: Repo
  // ##################################################################

  // ##################################################################
  // # BEGIN: ShutdownHook
  // ##################################################################

  private static final class ShutdownHookHolder {

    static final App.ShutdownHook INSTANCE = App.ShutdownHook.create(config -> config.noteSink(noteSink()));

  }

  public static void shutdownHook(AutoCloseable closeable) {
    ShutdownHookHolder.INSTANCE.register(closeable);
  }

  // ##################################################################
  // # END: ShutdownHook
  // ##################################################################

  // ##################################################################
  // # BEGIN: Tab
  // ##################################################################

  public sealed interface Tab permits YTab {

    void navigate(String path);

    String title();

    void dev();

  }

  // ##################################################################
  // # END: Tab
  // ##################################################################

}