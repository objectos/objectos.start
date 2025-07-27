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
package objectos.start;

import java.util.Map;
import objectos.start.app.Routes;
import objectos.way.App;
import objectos.way.Http;

public final class StartProd extends Start {

  public StartProd(Map<String, Object> bootOptions) {
    super(bootOptions);
  }

  @Override
  final void injectorStage(App.Injector.Options ctx) {
    // noop
  }

  @Override
  final Http.Handler serverHandler(App.Injector injector) {
    final Http.Routing.Module module;
    module = new Routes(injector);

    return Http.Handler.of(module);
  }

}
