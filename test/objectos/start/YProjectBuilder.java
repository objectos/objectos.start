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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class YProjectBuilder implements Y.Project.Options {

  private static final AtomicInteger PORTS = new AtomicInteger(4000);

  private final String basedirName = "objectos.test";

  private final Map<String, String> files = new HashMap<>();

  final int port = PORTS.getAndIncrement();

  final Path root = Y.nextTempDir();

  YProjectBuilder() {
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

  @Override
  public final void addFile(String relativePath, String contents) {
    final String maybeExisting;
    maybeExisting = files.put(relativePath, contents);

    if (maybeExisting != null) {
      throw new IllegalArgumentException(relativePath + " has already been defined");
    }
  }

  final Path basedir() {
    try {
      final Path basedir;
      basedir = root.resolve(basedirName);

      Files.createDirectory(basedir);

      for (Map.Entry<String, String> entry : files.entrySet()) {
        final String relativePath;
        relativePath = entry.getKey();

        final Path file;
        file = basedir.resolve(relativePath);

        final Path parent;
        parent = file.getParent();

        Files.createDirectories(parent);

        final String contents;
        contents = entry.getValue();

        Files.writeString(file, contents, StandardOpenOption.CREATE_NEW);
      }

      return basedir;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
