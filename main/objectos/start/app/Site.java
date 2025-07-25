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

import objectos.start.app.Ui.Page;
import objectos.way.App;
import objectos.way.Css;
import objectos.way.Http;
import objectos.way.Web;

public final class Site implements Http.Routing.Module {

  private final App.Injector injector;

  public Site(App.Injector injector) {
    this.injector = injector;
  }

  @Override
  public final void configure(Http.Routing routing) {
    routing.path("/", path -> {
      path.allow(Http.Method.GET, this::home);
    });

    // static (or semi-static) resources
    final Web.Resources webResources;
    webResources = injector.getInstance(Web.Resources.class);

    routing.path("/script.js", webResources::handlePath);

    routing.path("/styles.css", path -> {
      // in prod, we serve the file from the filesystem
      // in dev, we generate the file on each request
      path.allow(Http.Method.GET, this::styles);
    });
  }

  private void home(Http.Exchange http) {
    final Project.Model project;
    project = injector.getInstance(Project.Model.class);

    if (project.exists()) {
      throw new UnsupportedOperationException("Implement me");
    } else {
      final Page page;
      page = new Page("Welcome!");

      http.ok(page);
    }
  }

  private void styles(Http.Exchange http) {
    final Css.StyleSheet styles;
    styles = Ui.styles(injector);

    http.ok(styles);
  }

}