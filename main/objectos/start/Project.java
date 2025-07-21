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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/// The Objectos Start __Project__ namespace.
final class Project {

  record Coordinates(String group, String artifact, String version) implements Writable {
    @Override
    public final void writeTo(Writer w) throws IOException {
      w.header("coordinates");
      w.keyValue("group", group);
      w.keyValue("artifact", artifact);
      w.keyValue("version", version);
    }
  }

  /// Represents an Objectos Way project.
  static final class Model {

    private final Charset charset = StandardCharsets.UTF_8;

    private Coordinates coordinates;

    private final Path file;

    private Model(Path file) {
      this.file = file;
    }

    public static Model load(Path file) {
      Objects.requireNonNull(file, "file == null");

      return new Model(file);
    }

    public final boolean exists() {
      synchronized (this) {
        return Files.exists(file);
      }
    }

    public final void coordinates(Coordinates value) throws IOException {
      Objects.requireNonNull(value, "value == null");

      synchronized (this) {
        if (!Objects.equals(coordinates, value)) {
          coordinates = value;

          save();
        }
      }
    }

    private void save() throws IOException {
      final Path fileNamePath;
      fileNamePath = file.getFileName();

      final String tmpFileName;
      tmpFileName = fileNamePath.toString() + ".tmp";

      final Path tmp;
      tmp = file.resolveSibling(tmpFileName);

      try (BufferedWriter w = Files.newBufferedWriter(tmp, charset, StandardOpenOption.CREATE_NEW)) {
        save0(
            new Writer(w)
        );
      }

      Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
    }

    private void save0(Writer w) throws IOException {
      coordinates.writeTo(w);
    }

  }

  private static final class Writer {

    private final BufferedWriter w;

    public Writer(BufferedWriter w) {
      this.w = w;
    }

    public final void header(String name) throws IOException {
      w.write('[');
      w.write(name);
      w.write(']');
      w.newLine();
    }

    public final void keyValue(String name, String value) throws IOException {
      w.write(name);
      w.write(" = \"");
      w.write(value);
      w.write("\"");
      w.newLine();
    }

  }

  private interface Writable {

    void writeTo(Writer w) throws IOException;

  }

  private Project() {}

}
