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


import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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

/// Bootstraps Objectos Start.
///
/// Objectos Start generates, builds and manages one or more Objectos Way applications.
///
/// This class is not part of the Objectos Start JAR file.
/// It is placed in the main source tree to ease its development.
final class Way {

  private byte[] buffer;

  private final Clock clock = Clock.systemDefaultZone();

  private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private MessageDigest digest;

  private final HexFormat hexFormat = HexFormat.of();

  private HttpClient httpClient;

  private int int0;

  private PrintStream logger;

  private Object object0;

  private Object object1;

  private Options options;

  private byte state;

  private final String version;

  private final String waySha1;

  private Way(String version, String waySha1) {
    this.version = version;

    this.waySha1 = waySha1;
  }

  public static void main(String[] args) {
    final Way way;
    way = new Way("0.2.5", "e5a9574a4b58c9af8cc4c84e2f3e032786c32fa4");

    way.start(args);
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
  static final byte $BOOT_DEPS_DOWNLOAD = 7;
  static final byte $BOOT_DEPS_CHECKSUM = 8;

  static final byte $RUNNING = 9;
  static final byte $ERROR = 10;

  private void start(String[] args) {
    object0 = args;

    state = $OPTIONS;

    while (state < $RUNNING) {
      state = execute(state);
    }
  }

  final byte execute(byte state) {
    return switch (state) {
      case $OPTIONS -> executeOptions();
      case $OPTIONS_PARSE -> executeOptionsParse();

      case $INIT -> executeInit();
      case $INIT_TRY -> executeInitTry();

      case $BOOT_DEPS -> executeBootDeps();
      case $BOOT_DEPS_HAS_NEXT -> executeBootDepsHasNext();
      case $BOOT_DEPS_EXISTS -> executeBootDepsExists();
      case $BOOT_DEPS_DOWNLOAD -> executeBootDepsDownload();
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

      STRING,

      URI;

    }

    enum Source {
      COMMAND_LINE,

      DEFAULT;
    }

    final Kind kind;
    final String name;

    Object value;
    Source source;

    Option(Kind kind, String name, Object defaultValue) {
      this.kind = kind;
      this.name = name;
      this.value = defaultValue;

      source = Source.DEFAULT;
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

    final void parse(Source source, String rawValue) {
      value = switch (kind) {
        case DURATION -> Duration.parse(rawValue);
        case INTEGER -> Integer.valueOf(rawValue);
        case PATH -> Path.of(rawValue);
        case STRING -> rawValue;
        case URI -> URI.create(rawValue);
      };

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

    final URI uri() {
      return value(Kind.URI);
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

    final Map<String, Option> byName = new LinkedHashMap<>();
    int maxLength = 0;

    final Option basedir = path("--basedir", Path.of(System.getProperty("user.dir", "")));

    final Option bufferSize = integer("--buffer-size", 16 * 1024);

    final Option httpConnectTimout = duration("--http-connect-timeout", Duration.ofSeconds(10));

    final Option httpRequestTimout = duration("--http-request-timeout", Duration.ofMinutes(1));

    final Option repoLocal = path("--repo-local", basedir.path().resolve(".objectos/repository"));

    final Option repoRemote = uri("--repo-remote", URI.create("https://repo.maven.apache.org/maven2/"));

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

    private Option uri(String name, URI value) {
      return opt(Option.Kind.URI, name, value);
    }

    private Option opt(Option.Kind kind, String name, Object defaultValue) {
      final Option option;
      option = new Option(kind, name, defaultValue);

      byName.put(name, option);

      maxLength = Math.max(maxLength, name.length());

      return option;
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

      option.parse(Option.Source.COMMAND_LINE, rawValue);

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
    logger = System.out;

    logInfo("Objectos Start v%s", version);

    final String format;
    format = "%-" + options.maxLength + "s : %s [%s]";

    System.out.println(format);

    for (Option option : options.values()) {
      logInfo(format, option.name, option.value, option.source);
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
        new Artifact("br.com.objectos", "objectos.way", version, waySha1)
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
      return $BOOT_DEPS_DOWNLOAD;
    } else {
      return $BOOT_DEPS_CHECKSUM;
    }
  }

  private byte executeBootDepsDownload() {
    final Artifact dep;
    dep = (Artifact) object1;

    final URI uri;
    uri = dep.toURI();

    final Path file;
    file = dep.local();

    logInfo("DEP %s -> %s", uri, file);

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

    try {
      final HttpClient client;
      client = httpClient();

      client.send(request, bodyHandler);

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
      logError("Checksum mismatch for %s: got %s", file, sha1);

      return $ERROR;
    }

    logInfo("CHK %s", file);

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
      final String path;
      path = groupId.replace('.', '/')
          + "/" + artifactId
          + "/" + version
          + "/" + artifactId + "-" + version + ".jar";

      final Option option;
      option = options.repoRemote;

      final URI repoRemote;
      repoRemote = option.uri();

      return repoRemote.resolve(path);
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

  private void log0(System.Logger.Level level, String message) {
    final LocalDateTime now;
    now = LocalDateTime.now(clock);

    final String time;
    time = dateFormat.format(now);

    final String markerName;
    markerName = level.getName();

    logger.format("%s %-5s : %s%n", time, markerName, message);
  }

  private void logInfo(String message) {
    log0(INFO, message);
  }

  private void logInfo(String format, Object... args) {
    logInfo(
        String.format(format, args)
    );
  }

  private void logError(String message) {
    log0(ERROR, message);
  }

  private void logError(String format, Object... args) {
    logError(
        String.format(format, args)
    );
  }

  private byte toError(String message, Throwable t) {
    throw new UnsupportedOperationException(message, t);
  }

  // ##################################################################
  // # END: Logging
  // ##################################################################

}