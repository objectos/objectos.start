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
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import objectos.way.Toml;

final class ProjectModel implements Project.Model {

  static final byte _NEW = 0;

  static final byte _IO_ERROR = 1;

  static final byte _XML_ERROR = 2;

  static final byte _SYNC = 3;

  private Project.Config config;

  @SuppressWarnings("unused")
  private Exception error;

  private final Path file;

  private byte state;

  ProjectModel(Path file) {
    this.file = file;
  }

  static Project.Model load(Path file) {
    final ProjectModel model;
    model = new ProjectModel(file);

    model.reload();

    return model;
  }

  @Override
  public final boolean exists() {
    return state >= _SYNC;
  }

  @Override
  public final Project.Coordinates coordinates() {
    synchronized (this) {
      return config().coordinates();
    }
  }

  @Override
  public final void coordinates(Project.Coordinates value) throws IOException {
    Objects.requireNonNull(value, "value == null");

    synchronized (this) {
      final Project.Config cfg;
      cfg = config();

      config = cfg.with(value);

      saveIf(cfg);
    }
  }

  private Project.Config config() {
    if (config == null) {
      throw new IllegalStateException("config has not been loaded");
    }

    return config;
  }

  private void reload() {
    synchronized (this) {
      if (!Files.exists(file)) {
        config = Project.Config.empty();

        state = _NEW;

        return;
      }

      try (Toml.Reader reader = Toml.Reader.create(opts -> {
        opts.file(file);

        opts.lookup(MethodHandles.lookup());
      })) {

        config = reader.readRecord(Project.Config.class);

        state = _SYNC;

      } catch (IOException e) {
        state = _IO_ERROR;

        error = e;
      }
    }
  }

  private void saveIf(Project.Config cfg) throws IOException {
    if (config != cfg) {
      save();
    }
  }

  private void save() throws IOException {
    final Path fileNamePath;
    fileNamePath = file.getFileName();

    final String tmpFileName;
    tmpFileName = fileNamePath.toString() + ".tmp";

    final Path tmp;
    tmp = file.resolveSibling(tmpFileName);

    final Path parent;
    parent = tmp.getParent();

    Files.createDirectories(parent);

    try (Toml.Writer w = Toml.Writer.create(opts -> {
      opts.file(tmp);

      opts.lookup(MethodHandles.lookup());
    })) {
      w.writeRecord(config);
    }

    Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
  }

}