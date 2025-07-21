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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import objectos.way.App;
import objectos.way.Io;
import objectos.way.Note;

final class Y {

  private Y() {}

  // ##################################################################
  // # BEGIN: Clock
  // ##################################################################

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

  public static final class Project {

    static final class Builder {

      private final Path root = Y.nextTempDir();

      private Builder() {
        try {
          final Path originalWay;
          originalWay = Path.of("main", "objectos", "start", "Way.java");

          final String originalContents;
          originalContents = Files.readString(originalWay, StandardCharsets.UTF_8);

          final String contents;
          contents = originalContents.replace("package objectos.start;", "");

          addFile("Way.java", contents);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }

      public final void addFile(String relativePath, String contents) {
        try {
          final Path file;
          file = root.resolve(relativePath);

          final Path parent;
          parent = file.getParent();

          Files.createDirectories(parent);

          Files.writeString(file, contents, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }

      private Project build() {
        return new Project(this);
      }

    }

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

    private final Notes notes = Notes.get();

    private final Note.Sink noteSink = Y.noteSink();

    private Process process;

    private final Path root;

    private Project(Builder builder) {
      this.root = builder.root;
    }

    public final Set<String> ls() {
      try (Stream<Path> walk = Files.walk(root)) {
        return walk
            .filter(path -> !path.equals(root))
            .filter(Files::isRegularFile)
            .map(root::relativize)
            .map(Object::toString)
            .collect(Collectors.toUnmodifiableSet());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    public final String readString(String path) {
      try {
        final Path file;
        file = resolve(path);

        return Files.readString(file);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    public final Path resolve(String path) {
      return root.resolve(path);
    }

    public final void waitFor() {
      try {
        process.waitFor();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    public final void way(String... args) {
      final List<String> cmd;
      cmd = new ArrayList<>();

      cmd.add("java");

      cmd.add("Way.java");

      for (String arg : args) {
        cmd.add(arg);
      }

      try {
        final ProcessBuilder builder;
        builder = new ProcessBuilder(cmd);

        builder.directory(root.toFile());

        process = builder.start();

        Thread.ofVirtual().start(this::stderr);
        Thread.ofVirtual().start(this::stdout);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void stderr() {
      try (BufferedReader reader = process.errorReader()) {
        String line;
        while ((line = reader.readLine()) != null) {
          System.out.println(line);
        }
      } catch (IOException e) {
        noteSink.send(notes.ioException, e);
      }
    }

    private void stdout() {
      try (BufferedReader reader = process.inputReader()) {
        String line;
        while ((line = reader.readLine()) != null) {
          System.out.println(line);
        }
      } catch (IOException e) {
        noteSink.send(notes.ioException, e);
      }
    }

  }

  public static Project project(Consumer<? super Project.Builder> opts) {
    final Project.Builder builder;
    builder = new Project.Builder();

    opts.accept(builder);

    return builder.build();
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
  // # BEGIN: Way.Logger
  // ##################################################################

  public static final class WayLogger implements Appendable {

    private final List<String> logs = new ArrayList<>();

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

  public static WayLogger wayLogger() {
    return new WayLogger();
  }

  // ##################################################################
  // # END: Way.Logger
  // ##################################################################

  // ##################################################################
  // # BEGIN: Way.Meta
  // ##################################################################

  static final class Meta {

    final String h2Sha1 = "4fcc05d966ccdb2812ae8b9a718f69226c0cf4e2"; // sed:H2_SHA1

    final String h2Version = "2.3.232"; // sed:H2_VERSION

    final String startSha1 = "9cf26b0f2692c4ee94a4a71a9a0122a17d776270"; // sed:START_SHA1

    final String startVersion = "0.1.0-SNAPSHOT"; // sed:START_VERSION

    final String waySha1 = "2e53952e785111740060d6d2083045715831c31c"; // sed:WAY_SHA1

    final String wayVersion = "0.2.6-SNAPSHOT"; // sed:WAY_VERSION

  }

  public static final Meta META = new Meta();

  // ##################################################################
  // # END: Way.Meta
  // ##################################################################

  // ##################################################################
  // # BEGIN: WayTester
  // ##################################################################

  public static final class WayTester {

    private final WayLogger logger;

    private final Way way;

    private WayTester(WayLogger logger, Way way) {
      this.logger = logger;
      this.way = way;
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

  public static WayTester wayTester() {
    final Y.WayLogger logger;
    logger = Y.wayLogger();

    final Way way;
    way = new Way();

    way.clock(clockIncMillis(2025, 1, 1));

    way.logger(logger);

    return new WayTester(logger, way);
  }

  // ##################################################################
  // # END: WayTester
  // ##################################################################

}