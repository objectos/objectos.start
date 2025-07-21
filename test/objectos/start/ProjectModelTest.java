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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.IOException;
import java.nio.file.Path;
import org.testng.annotations.Test;

public class ProjectModelTest {

  @Test
  public void testCase01() throws IOException {
    final Y.Project project;
    project = Y.project(opts -> {});

    final Path file;
    file = project.resolve("Way.toml");

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
        project.readString("Way.toml"),

        """
        [coordinates]
        group = "br.com.objectos"
        artifact = "objectos.test"
        version = "1.0.0-SNAPSHOT"
        """
    );
  }

}