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
package objectos.start.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import objectos.way.App;

/// The Objectos Start __Project__ namespace.
public final class Project {

  record Config(
      Coordinates coordinates
  ) {

    static Config empty() {
      return new Config(null);
    }

    public final Coordinates coordinates() {
      check(coordinates, "coordinates == null");

      return coordinates;
    }

    final Config with(Coordinates value) {
      if (Objects.equals(coordinates, value)) {
        return this;
      } else {
        return new Config(value);
      }
    }

    private void check(Object value, String message) {
      if (value == null) {
        throw new IllegalStateException(message);
      }
    }

  }

  record Coordinates(
      String group,
      String artifact,
      String version
  ) {}

  /// Represents an Objectos Way project.
  @App.DoNotReload
  public sealed interface Model permits ProjectModel {

    static Model load(Path file) {
      return ProjectModel.load(file);
    }

    boolean exists();

    Coordinates coordinates();

    void coordinates(Coordinates value) throws IOException;

  }

  private Project() {}

}
