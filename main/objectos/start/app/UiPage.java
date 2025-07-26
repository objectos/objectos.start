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

import java.util.Objects;
import objectos.start.app.Ui.Page;
import objectos.way.Css;
import objectos.way.Html;

@Css.Source
final class UiPage extends Html.Template {

  private final String title;

  UiPage(Page pojo) {
    title = Objects.requireNonNull(pojo.title, "title == null");
  }

  @Override
  protected final void render() {
    doctype();
    html(
        head(
            link(rel("stylesheet"), type("text/css"), href("/styles.css")),
            script(src("/script.js")),
            title(title)
        ),
        body(
            h1("Welcome, Foo")
        )
    );
  }

}
