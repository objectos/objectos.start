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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Bootstraps Objectos Start.
///
/// Objectos Start generates, builds and manages one or more Objectos Way applications.
///
/// This class is not part of the Objectos Start JAR file.
/// It is placed in the main source tree to ease its development.
final class Way {

  static final class Meta {

    final String h2Sha1 = "4fcc05d966ccdb2812ae8b9a718f69226c0cf4e2"; // sed:H2_SHA1

    final String h2Version = "2.3.232"; // sed:H2_VERSION

    final String startSha1 = "6a72bc1e83e82c5fa9c9199a0303b1bb31678839"; // sed:START_SHA1

    final String startVersion = "0.1.0-SNAPSHOT"; // sed:START_VERSION

    final String waySha1 = "733728007861811ee00cb5da51b7e898749845cb"; // sed:WAY_SHA1

    final String wayVersion = "0.2.6-SNAPSHOT"; // sed:WAY_VERSION

  }

  private byte[] buffer;

  private Clock clock;

  private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private MessageDigest digest;

  private final HexFormat hexFormat = HexFormat.of();

  private HttpClient httpClient;

  private int int0;

  private Appendable logger;

  private final Meta meta = new Meta();

  private Object object0;

  private Object object1;

  private Options options;

  private byte state;

  // visible for testing
  Way() {}

  public static void main(String... args) {
    final Way way;
    way = new Way();

    way.start(args);
  }

  // visible for testing
  final Closeable start(String[] args) {
    object0 = args;

    execute($OPTIONS, $RUNNING);

    if (object0 instanceof Closeable c) {
      return c;
    } else {
      return null;
    }
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

  static final byte $LAYER = 9;

  static final byte $RUNNING = 10;
  static final byte $ERROR = 11;

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

      case $LAYER -> executeLayer();

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

    final Consumer<Option> validator;

    Object value;

    Option(Kind kind, String name, Consumer<Option> validator) {
      this.kind = kind;
      this.name = name;
      this.validator = validator;
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

    final void parse(String source, String rawValue) {
      value = switch (kind) {
        case DURATION -> Duration.parse(rawValue);

        case INTEGER -> Integer.valueOf(rawValue);

        case PATH -> Path.of(rawValue);

        case STRING -> rawValue;
      };

      this.source = source;
    }

    final void validate() {
      if (validator != null) {
        validator.accept(this);
      }
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

    final boolean unset() {
      return value == null;
    }

    final boolean set() {
      return value != null;
    }

    final void set(Object v) {
      source = "DEF";

      value = v;
    }

    final boolean trySet(Object defaultValue) {
      boolean result;
      result = false;

      if (value == null) {
        set(defaultValue);

        result = true;
      }

      return result;
    }

    final void allowedValues(Object... allowed) {
      for (Object o : allowed) {
        if (value.equals(o)) {
          return;
        }
      }

      final String values;
      values = Stream.of(allowed).map(Object::toString).collect(Collectors.joining(", "));

      throw new IllegalArgumentException(name + " allowed values: " + values);
    }

  }

  // ad-hoc enum so instances can be GC'ed after use.
  private class Options {

    // these must come first
    final Map<String, Option> byName = new LinkedHashMap<>();
    int maxLength = 0;
    private final List<String> startArgs = new ArrayList<>();

    // options: order is significant
    final Option stage = string("--stage", opt -> {
      if (!opt.trySet("prod")) {
        opt.allowedValues("dev", "prod", "test");
      }
    });

    final Option basedir = path("--basedir", opt -> {
      if (opt.unset()) {
        final String prop;
        prop = System.getProperty("user.dir", "");

        Path p;
        p = Path.of(prop);

        if (!p.isAbsolute()) {
          p = p.toAbsolutePath();
        }

        opt.set(p);
      }
    });

    final Option bufferSize = integer("--buffer-size", opt -> {
      opt.trySet(16 * 1024);
    });

    final Option classOutput = path("--class-output", opt -> {
      if (opt.set()) {
        final String stageName;
        stageName = stage.string();

        if ("prod".equals(stageName)) {
          throw new IllegalArgumentException("--class-output must not be set with --stage prod");
        }
      }
    });

    final Option httpConnectTimout = duration("--http-connect-timeout", opt -> {
      if (opt.unset()) {
        opt.set(Duration.ofSeconds(10));
      }
    });

    final Option httpRequestTimout = duration("--http-request-timeout", opt -> {
      if (opt.unset()) {
        opt.set(Duration.ofMinutes(1));
      }
    });

    final Option workdir = path("--workdir", opt -> {
      if (opt.unset()) {
        opt.set(
            basedir.path().resolve(".objectos")
        );
      }
    });

    final Option repoBoot = path("--repo-boot", opt -> {
      if (opt.unset()) {
        opt.set(
            workdir.path().resolve("boot")
        );
      }
    });

    final Option repoRemote = string("--repo-remote", opt -> {
      if (!opt.trySet("https://repo.maven.apache.org/maven2/")) {
        final String repoRemote;
        repoRemote = opt.string();

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
      }
    });

    final Iterable<Option> values() {
      return byName.values();
    }

    private Option duration(String name, Consumer<Option> validator) {
      return opt(Option.Kind.DURATION, name, validator);
    }

    private Option integer(String name, Consumer<Option> validator) {
      return opt(Option.Kind.INTEGER, name, validator);
    }

    private Option path(String name, Consumer<Option> validator) {
      return opt(Option.Kind.PATH, name, validator);
    }

    private Option string(String name, Consumer<Option> validator) {
      return opt(Option.Kind.STRING, name, validator);
    }

    private Option opt(Option.Kind kind, String name, Consumer<Option> validator) {
      final Option option;
      option = new Option(kind, name, validator);

      byName.put(name, option);

      maxLength = Math.max(maxLength, name.length());

      return option;
    }

    final Map<String, Object> asMap() {
      final Map<String, Object> map;
      map = new HashMap<>();

      for (Map.Entry<String, Option> entry : byName.entrySet()) {
        final String key;
        key = entry.getKey();

        final Option value;
        value = entry.getValue();

        map.put(key, value.value);
      }

      return map;
    }

    final String[] startArgs() {
      return startArgs.toArray(String[]::new);
    }

    final void postpone(String arg) {
      startArgs.add(arg);
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

    final String arg;
    arg = args[int0++];

    final Map<String, Option> byName;
    byName = options.byName;

    final Option option;
    option = byName.get(arg);

    if (option == null) {

      options.postpone(arg);

      return $OPTIONS_PARSE;

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
    for (Option opt : options.values()) {
      opt.validate();
    }

    if (clock == null) {
      clock = Clock.systemDefaultZone();
    }

    if (logger == null) {
      logger = System.out;
    }

    logInfo("Objectos Start v%s", meta.startVersion);

    final String format;
    format = "(%3s) %-" + options.maxLength + "s %s";

    for (Option option : options.values()) {
      logInfo(format, option.source, option.name, option.value);
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

    // repoBoot
    try {
      final Option repoBoot;
      repoBoot = options.repoBoot;

      ensureDirectory(repoBoot.path());
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

    final String stage;
    stage = options.stage.string();

    switch (stage) {
      case "prod" -> {
        object0 = new Artifact[] {
            new Artifact("br.com.objectos", "objectos.way", meta.wayVersion, meta.waySha1),

            new Artifact("br.com.objectos", "objectos.start", meta.startVersion, meta.startSha1)
        };
      }

      default -> {
        object0 = new Artifact[] {
            new Artifact("br.com.objectos", "objectos.way", meta.wayVersion, meta.waySha1)
        };
      }
    }

    return $BOOT_DEPS_HAS_NEXT;
  }

  private byte executeBootDepsHasNext() {
    final Artifact[] deps;
    deps = (Artifact[]) object0;

    if (int0 < deps.length) {
      object1 = deps[int0++];

      return $BOOT_DEPS_EXISTS;
    } else {
      return $LAYER;
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

    logInfo("DEP %s -> %s", uri, file);

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
  // # BEGIN: Module Layer
  // ##################################################################

  private byte executeLayer() {
    // boot layer
    final ModuleLayer boot;
    boot = ModuleLayer.boot();

    final Configuration bootConfig;
    bootConfig = boot.configuration();

    // our module finder
    final Path location;
    location = options.repoBoot.path();

    final String className;

    final ModuleFinder finder;

    final String stage;
    stage = options.stage.string();

    switch (stage) {
      case "prod" -> {
        className = "objectos.start.StartProd";

        finder = ModuleFinder.of(location);
      }

      case "dev" -> {
        className = "objectos.start.StartDev";

        finder = ModuleFinder.of(location, options.classOutput.path());
      }

      case "test" -> {
        className = "objectos.start.StartTest";

        finder = ModuleFinder.of(location, options.classOutput.path());
      }

      default -> throw new AssertionError("Unexpected stage " + stage);
    }

    final ModuleFinder afterFinder;
    afterFinder = ModuleFinder.of();

    // only objectos.start
    final Set<String> roots;
    roots = Set.of("objectos.start");

    // our config
    final Configuration configuration;
    configuration = bootConfig.resolve(finder, afterFinder, roots);

    // our layer
    final ClassLoader systemClassLoader;
    systemClassLoader = ClassLoader.getSystemClassLoader();

    final ModuleLayer layer;
    layer = boot.defineModulesWithManyLoaders(configuration, systemClassLoader);

    // our loader
    final ClassLoader loader;
    loader = layer.findLoader("objectos.start");

    final Class<?> startClass;

    try {
      startClass = loader.loadClass(className);
    } catch (ClassNotFoundException | SecurityException e) {
      return toError("Failed to load the Objectos Start class", e);
    }

    final Constructor<?> constructor;

    try {
      constructor = startClass.getConstructor(Map.class);
    } catch (NoSuchMethodException | SecurityException e) {
      return toError("Failed to reflect the constructor", e);
    }

    final Object startInstance;

    try {
      final Map<String, Object> map;
      map = options.asMap();

      map.put("logger", logger);

      startInstance = constructor.newInstance(map);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      return toError("Failed to create an Objectos Start instance", e);
    }

    final Method startMethod;

    try {
      startMethod = startClass.getMethod("start", String[].class);
    } catch (NoSuchMethodException | SecurityException e) {
      return toError("Failed to reflect the start method", e);
    }

    try {
      final Object args;
      args = options.startArgs();

      startMethod.invoke(startInstance, args);
    } catch (IllegalAccessException | InvocationTargetException e) {
      return toError("Failed to invoke the start method", e);
    }

    object0 = startInstance;

    return $RUNNING;
  }

  // ##################################################################
  // # END: Module Layer
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
        option = options.repoBoot;

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

  private void log0(System.Logger.Level level, String message) {
    try {
      final LocalDateTime now;
      now = LocalDateTime.now(clock);

      final String time;
      time = dateFormat.format(now);

      final String markerName;
      markerName = level.getName();

      final String log;
      log = String.format("%s %-5s %s%n", time, markerName, message);

      logger.append(log);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to log message", e);
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

  final void clock(Clock value) {
    clock = value;
  }

  final void object0(Object value) {
    object0 = value;
  }

  final void logger(Appendable value) {
    logger = value;
  }

  // ##################################################################
  // # END: Testing API
  // ##################################################################

}