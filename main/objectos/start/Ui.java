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

import objectos.way.Html;
import objectos.way.Http;

final class Ui {

  static final class Module implements Http.Routing.Module {
    @Override
    public final void configure(Http.Routing routing) {
      routing.path("/", path -> {
        path.allow(Http.Method.GET, this::welcome);
      });
    }

    private void welcome(Http.Exchange http) {
      final Page page = new Page("Welcome!");

      http.ok(page);
    }
  }

  static final class Page extends Html.Template {
    private final String title;

    Page(String title) {
      this.title = title;
    }

    @Override
    protected final void render() {
      doctype();
      html(
          head(
              title(title)
          ),
          body(
          )
      );
    }
  }

}