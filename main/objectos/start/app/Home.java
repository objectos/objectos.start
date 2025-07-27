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

import objectos.way.App;
import objectos.way.Css;
import objectos.way.Http;

@Css.Source
final class Home implements Http.Handler {

  private final App.Injector injector;

  Home(App.Injector injector) {
    this.injector = injector;
  }

  @Override
  public final void handle(Http.Exchange http) {
    switch (http.method()) {
      case GET -> get(http);

      case POST -> post(http);

      default -> http.allow(Http.Method.GET, Http.Method.POST);
    }
  }

  private void get(Http.Exchange http) {
    final Project.Model project;
    project = injector.getInstance(Project.Model.class);

    if (!project.exists()) {
      getWelcome(http);
    } else {
      throw new UnsupportedOperationException("Implement me");
    }
  }

  private void getWelcome(Http.Exchange http) {
    http.ok(Ui.page(page -> {
      page.title = "Welcome!";

      page.body = h -> {
        h.main(
            h.css("""

            """),

            h.h1(
                h.css("""

                """),

                h.text(
                    h.testableH1("Objectos Start")
                )
            )
        );
      };
    }));
  }

  private void post(Http.Exchange http) {

  }

}
