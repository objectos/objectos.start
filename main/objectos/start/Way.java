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

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/// Bootstraps Objectos Start.
///
/// Objectos Start generates, builds and manages one or more Objectos Way applications.
///
/// This class is not part of the Objectos Start JAR file.
/// It is placed in the main source tree to ease its development.
final class Way {

  static final class Meta {

    final String sha1Start = "2306e9030df5f0dcf89454e3ea8a2f3ea3d7a916"; /* sed:SHA1_SELF */

    final String sha1Way = "66032cc22fe7d13495f530278b8fcfb99e52cf1d"; /* sed:SHA1_WAY */

    final String version = "0.2.6-SNAPSHOT"; // sed:VERSION

  }

  private byte[] buffer;

  private MessageDigest digest;

  private final HexFormat hexFormat = HexFormat.of();

  private HttpClient httpClient;

  private int int0;

  private Logger logger;

  private final Meta meta = new Meta();

  private Object object0;

  private Object object1;

  private Options options;

  private byte state;

  // visible for testing
  Way() {}

  public static void main(String[] args) {
    final Way way;
    way = new Way();

    way.start(args);
  }

  private void start(String[] args) {
    object0 = args;

    execute($OPTIONS, $RUNNING);
  }

  // ##################################################################
  // # BEGIN: State Machine
  // ##################################################################

  static final byte $OPTIONS = 0;
  static final byte $OPTIONS_PARSE = 1;

  static final byte $INIT = 2;
  static final byte $INIT_TRY = 3;

  static final byte $BOOT_DEPS = 4;
  static final byte $BOOT_DEPS_HAS_NEXT = 5;
  static final byte $BOOT_DEPS_EXISTS = 6;
  static final byte $BOOT_DEPS_FETCH = 7;
  static final byte $BOOT_DEPS_CHECKSUM = 8;

  static final byte $RUNNING = 9;
  static final byte $ERROR = 10;

  final void execute(byte from, byte to) {
    state = from;

    while (state < to) {
      execute();
    }
  }

  private void execute() {
    state = switch (state) {
      case $OPTIONS -> executeOptions();
      case $OPTIONS_PARSE -> executeOptionsParse();

      case $INIT -> executeInit();
      case $INIT_TRY -> executeInitTry();

      case $BOOT_DEPS -> executeBootDeps();
      case $BOOT_DEPS_HAS_NEXT -> executeBootDepsHasNext();
      case $BOOT_DEPS_EXISTS -> executeBootDepsExists();
      case $BOOT_DEPS_FETCH -> executeBootDepsFetch();
      case $BOOT_DEPS_CHECKSUM -> executeBootDepsChecksum();

      default -> throw new AssertionError("Unexpected state=" + state);
    };
  }

  // ##################################################################
  // # END: State Machine
  // ##################################################################

  // ##################################################################
  // # BEGIN: Options
  // ##################################################################

  private static final class Option {

    enum Kind {

      DURATION,

      INTEGER,

      PATH,

      STRING;

    }

    final Kind kind;

    final String name;

    // DEF = Default
    // CLI = Command Line
    // SYS = System
    String source;

    Function<? super Object, ? extends Object> validator;

    Object value;

    Option(Kind kind, String name, Object defaultValue) {
      this.kind = kind;
      this.name = name;
      this.value = defaultValue;

      source = "DEF";
    }

    @Override
    public final boolean equals(Object obj) {
      return obj == this || obj instanceof Option that
          && name.equals(that.name);
    }

    @Override
    public final int hashCode() {
      return name.hashCode();
    }

    final Option validator(Function<? super Object, ? extends Object> value) {
      validator = value;

      return this;
    }

    final void parse(String source, String rawValue) {
      value = switch (kind) {
        case DURATION -> Duration.parse(rawValue);

        case INTEGER -> Integer.valueOf(rawValue);

        case PATH -> Path.of(rawValue);

        case STRING -> rawValue;
      };

      if (validator != null) {
        value = validator.apply(value);
      }

      this.source = source;
    }

    final Duration duration() {
      return value(Kind.DURATION);
    }

    final int intValue() {
      final Integer v;
      v = value(Kind.INTEGER);

      return v.intValue();
    }

    final Path path() {
      return value(Kind.PATH);
    }

    final String string() {
      return value(Kind.STRING);
    }

    @SuppressWarnings("unchecked")
    private <T> T value(Kind expected) {
      if (kind != expected) {
        throw new UnsupportedOperationException("Operation is only allowed for kind=" + expected + " but kind=" + kind);
      }

      return (T) value;
    }

  }

  // ad-hoc enum so instances can be GC'ed after use.
  private class Options {

    // these must come first
    final Map<String, Option> byName = new LinkedHashMap<>();
    int maxLength = 0;

    // options
    final Option basedir = path("--basedir", Path.of(""));

    final Option bufferSize = integer("--buffer-size", 16 * 1024);

    final Option httpConnectTimout = duration("--http-connect-timeout", Duration.ofSeconds(10));

    final Option httpRequestTimout = duration("--http-request-timeout", Duration.ofMinutes(1));

    final Option repoLocal = path("--repo-local", basedir.path().resolve(".objectos/repository"));

    final Option repoRemote = string("--repo-remote", "https://repo.maven.apache.org/maven2/")
        .validator(this::repoRemote);

    final Iterable<Option> values() {
      return byName.values();
    }

    private Option duration(String name, Duration value) {
      return opt(Option.Kind.DURATION, name, value);
    }

    private Option integer(String name, int value) {
      return opt(Option.Kind.INTEGER, name, value);
    }

    private Option path(String name, Path value) {
      return opt(Option.Kind.PATH, name, value);
    }

    private Option string(String name, String value) {
      return opt(Option.Kind.STRING, name, value);
    }

    private Option opt(Option.Kind kind, String name, Object defaultValue) {
      final Option option;
      option = new Option(kind, name, defaultValue);

      byName.put(name, option);

      maxLength = Math.max(maxLength, name.length());

      return option;
    }

    private Object repoRemote(Object obj) {
      final String repoRemote;
      repoRemote = (String) obj;

      if (repoRemote.isEmpty()) {
        throw new IllegalArgumentException("--repo-remote must not be empty");
      }

      if (repoRemote.isBlank()) {
        throw new IllegalArgumentException("--repo-remote must not be blank");
      }

      final int length;
      length = repoRemote.length();

      final char last;
      last = repoRemote.charAt(length - 1);

      if (last != '/') {
        throw new IllegalArgumentException("--repo-remote path must end in a '/' character, but was: " + repoRemote);
      }

      return repoRemote;
    }

  }

  private byte executeOptions() {
    options = new Options();

    int0 = 0;

    return $OPTIONS_PARSE;
  }

  private byte executeOptionsParse() {
    final String[] args;
    args = (String[]) object0;

    if (int0 == args.length) {
      // no more command line args
      return $INIT;
    }

    final String keyName;
    keyName = args[int0++];

    final Map<String, Option> byName;
    byName = options.byName;

    final Option option;
    option = byName.get(keyName);

    if (option == null) {
      throw new UnsupportedOperationException("Implement me :: unknown key :: " + keyName);
    }

    if (int0 == args.length) {
      throw new UnsupportedOperationException("Implement me :: no value");
    }

    try {
      final String rawValue;
      rawValue = args[int0++];

      option.parse("CLI", rawValue);

      return $OPTIONS_PARSE;
    } catch (RuntimeException parseException) {
      throw new UnsupportedOperationException("Implement me", parseException);
    }
  }

  // ##################################################################
  // # END: Options
  // ##################################################################

  // ##################################################################
  // # BEGIN: Init
  // ##################################################################

  private byte executeInit() {
    if (logger == null) {
      logger = new Logger();
    }

    logger.info("Objectos Start v%s", meta.version);

    final String format;
    format = "(%3s) %-" + options.maxLength + "s %s";

    for (Option option : options.values()) {
      logger.info(format, option.source, option.name, option.value);
    }

    return $INIT_TRY;
  }

  private byte executeInitTry() {
    // buffer
    final Option optionBufferSize;
    optionBufferSize = options.bufferSize;

    final int bufferSize;
    bufferSize = optionBufferSize.intValue();

    buffer = new byte[bufferSize];

    // digest
    try {
      digest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      return toError("Failed obtain the SHA-1 digest instance", e);
    }

    // repoLocal
    try {
      final Option repoLocal;
      repoLocal = options.repoLocal;

      ensureDirectory(repoLocal.path());
    } catch (IOException e) {
      return toError("Failed to create local repository directory", e);
    }

    return $BOOT_DEPS;
  }

  // ##################################################################
  // # END: Init
  // ##################################################################

  // ##################################################################
  // # BEGIN: Boot Deps
  // ##################################################################

  private byte executeBootDeps() {
    int0 = 0;

    object0 = new Artifact[] {
        new Artifact("br.com.objectos", "objectos.way", meta.version, meta.sha1Way),

        new Artifact("br.com.objectos", "objectos.start", meta.version, meta.sha1Start)
    };

    return $BOOT_DEPS_HAS_NEXT;
  }

  private byte executeBootDepsHasNext() {
    final Artifact[] deps;
    deps = (Artifact[]) object0;

    if (int0 < deps.length) {
      object1 = deps[int0++];

      return $BOOT_DEPS_EXISTS;
    } else {
      return $RUNNING;
    }
  }

  private byte executeBootDepsExists() {
    final Artifact dep;
    dep = (Artifact) object1;

    if (!dep.exists()) {
      return $BOOT_DEPS_FETCH;
    } else {
      return $BOOT_DEPS_CHECKSUM;
    }
  }

  private byte executeBootDepsFetch() {
    final Artifact dep;
    dep = (Artifact) object1;

    final URI uri;
    uri = dep.toURI();

    final Path file;
    file = dep.local();

    logger.info("DEP %s -> %s", uri, file);

    final String scheme;
    scheme = uri.getScheme();

    try {
      if (scheme != null) {
        final Option httpRequestTimeout;
        httpRequestTimeout = options.httpRequestTimout;

        final HttpRequest request;
        request = HttpRequest.newBuilder()
            .GET()
            .uri(uri)
            .timeout(httpRequestTimeout.duration())
            .build();

        final BodyHandler<Path> bodyHandler;
        bodyHandler = HttpResponse.BodyHandlers.ofFile(file);

        final HttpClient client;
        client = httpClient();

        client.send(request, bodyHandler);

      } else {
        final String path;
        path = uri.getPath();

        final Path source;
        source = Path.of(path);

        Files.copy(source, file);
      }

      return $BOOT_DEPS_CHECKSUM;
    } catch (IOException | InterruptedException e) {
      return toError("Failed to download: " + uri, e);
    }
  }

  private byte executeBootDepsChecksum() {
    digest.reset();

    final Artifact dep;
    dep = (Artifact) object1;

    final Path file;
    file = dep.local();

    try (InputStream in = Files.newInputStream(file)) {
      while (true) {
        final int read;
        read = in.read(buffer);

        if (read == -1) {
          break;
        }

        digest.update(buffer, 0, read);
      }
    } catch (IOException e) {
      return toError("Failed to compute checksum: " + file, e);
    }

    final byte[] sha1Bytes;
    sha1Bytes = digest.digest();

    final String sha1;
    sha1 = hexFormat.formatHex(sha1Bytes);

    if (!sha1.equals(dep.sha1)) {
      logger.error("Checksum mismatch for %s: got %s", file, sha1);

      return $ERROR;
    }

    logger.info("CHK %s", file);

    return $BOOT_DEPS_HAS_NEXT;
  }

  // ##################################################################
  // # END: Boot Deps
  // ##################################################################

  // ##################################################################
  // # BEGIN: Artifact
  // ##################################################################

  private class Artifact {

    final String groupId;
    final String artifactId;
    final String version;
    final String sha1;

    private Path local;

    Artifact(String groupId, String artifactId, String version, String sha1) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
      this.sha1 = sha1;
    }

    final boolean exists() {
      return Files.exists(
          local()
      );
    }

    final URI toURI() {
      final Option option;
      option = options.repoRemote;

      final String repoRemote;
      repoRemote = option.string();

      final String path;
      path = groupId.replace('.', '/')
          + "/" + artifactId
          + "/" + version
          + "/" + artifactId + "-" + version + ".jar";

      return URI.create(repoRemote + path);
    }

    private Path local() {
      if (local == null) {
        final Option option;
        option = options.repoLocal;

        final Path repoLocal;
        repoLocal = option.path();

        local = repoLocal.resolve(sha1 + ".jar");
      }

      return local;
    }

  }

  // ##################################################################
  // # END: Artifact
  // ##################################################################

  // ##################################################################
  // # BEGIN: HTTP Client
  // ##################################################################

  private HttpClient httpClient() {
    if (httpClient == null) {
      final Option option;
      option = options.httpConnectTimout;

      final Duration connectTimeout;
      connectTimeout = option.duration();

      httpClient = HttpClient.newBuilder()
          .connectTimeout(connectTimeout)
          .build();
    }

    return httpClient;
  }

  // ##################################################################
  // # END: HTTP Client
  // ##################################################################

  // ##################################################################
  // # BEGIN: I/O
  // ##################################################################

  private void ensureDirectory(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      final Path parent;
      parent = directory.getParent();

      Files.createDirectories(parent);

      Files.createDirectory(directory);
    }
  }

  // ##################################################################
  // # END: I/O
  // ##################################################################

  // ##################################################################
  // # BEGIN: Logging
  // ##################################################################

  static class Logger {

    private final Clock clock;

    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    Logger() {
      this(Clock.systemDefaultZone());
    }

    Logger(Clock clock) {
      this.clock = clock;
    }

    final void info(String message) {
      log0(INFO, message);
    }

    final void info(String format, Object... args) {
      info(
          String.format(format, args)
      );
    }

    final void error(String message) {
      log0(ERROR, message);
    }

    final void error(String format, Object... args) {
      error(
          String.format(format, args)
      );
    }

    void print(String log) {
      System.out.println(log);
    }

    private void log0(System.Logger.Level level, String message) {
      final LocalDateTime now;
      now = LocalDateTime.now(clock);

      final String time;
      time = dateFormat.format(now);

      final String markerName;
      markerName = level.getName();

      final String log;
      log = String.format("%s %-5s %s", time, markerName, message);

      print(log);
    }

  }

  private byte toError(String message, Throwable t) {
    throw new UnsupportedOperationException(message, t);
  }

  // ##################################################################
  // # END: Logging
  // ##################################################################

  // ##################################################################
  // # BEGIN: Testing API
  // ##################################################################

  final void object0(Object value) {
    object0 = value;
  }

  final void logger(Logger value) {
    logger = value;
  }

  // ##################################################################
  // # END: Testing API
  // ##################################################################

}