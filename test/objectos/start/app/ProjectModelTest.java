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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.testng.annotations.Test;

public class ProjectModelTest {

  @Test
  public void testCase01() throws IOException {
    final Y.Project project;
    project = Y.project(opts -> {});

    final Path file;
    file = project.resolve(".objectos/project.toml");

    final Project.Model model;
    model = Project.Model.load(file);

    assertFalse(model.exists());

    final Project.Coordinates coordinates;
    coordinates = new Project.Coordinates(
        "br.com.objectos",
        "objectos.test",
        "1.0.0-SNAPSHOT"
    );

    model.coordinates(coordinates);

    assertEquals(
        project.readString(".objectos/project.toml"),

        """
        [coordinates]
        group = "br.com.objectos"
        artifact = "objectos.test"
        version = "1.0.0-SNAPSHOT"
        """
    );
  }

  @Test
  public void testCase02() {
    final Y.Project project;
    project = Y.project(opts -> {
      opts.addFile(".objectos/project.toml", """
      [coordinates]
      group = "br.com.objectos"
      artifact = "objectos.test"
      version = "1.0.0-SNAPSHOT"
      """);
    });

    final Path file;
    file = project.resolve(".objectos/project.toml");

    final Project.Model model;
    model = Project.Model.load(file);

    assertTrue(model.exists());

    assertEquals(
        model.coordinates(),

        new Project.Coordinates(
            "br.com.objectos",
            "objectos.test",
            "1.0.0-SNAPSHOT"
        )
    );
  }

}