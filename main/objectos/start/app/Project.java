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
import java.nio.file.Path;
import java.util.Objects;
import objectos.way.App;
import objectos.way.Web;

/// The Objectos Start __Project__ namespace.
@App.DoNotReload
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
  ) {

    static Web.FormSpec formSpec(String action) {
      return Web.FormSpec.create(form -> {
        form.action(action);

        form.textInput(input -> {
          input.name("group");
          input.maxLength(160);
          input.required();
        });

        form.textInput(input -> {
          input.name("artifact");
          input.maxLength(100);
          input.required();
        });

        form.textInput(input -> {
          input.name("version");
          input.maxLength(60);
          input.required();
        });
      });
    }

  }

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
