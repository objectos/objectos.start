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
