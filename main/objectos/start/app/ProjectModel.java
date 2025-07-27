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