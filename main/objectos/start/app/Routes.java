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
import objectos.way.Http;
import objectos.way.Web;

public class Routes implements Http.Routing.Module {

  protected final App.Injector injector;

  public Routes(App.Injector injector) {
    this.injector = injector;
  }

  @Override
  public final void configure(Http.Routing routing) {
    routing.path("/", path -> {
      path.allow(Http.Method.GET, Http.Handler.factory(Home::new, injector));
    });

    // static (or semi-static) resources
    final Web.Resources webResources;
    webResources = injector.getInstance(Web.Resources.class);

    routing.path("/script.js", webResources::handlePath);

    configureStage(routing);

    routing.handler(Http.Handler.notFound());
  }

  protected void configureStage(Http.Routing routing) {}

}