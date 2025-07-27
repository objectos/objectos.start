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

import java.util.Objects;
import objectos.way.Css;
import objectos.way.Html;

@Css.Source
final class UiPage extends Html.Template {

  private final String title;

  private final Html.Component body;

  UiPage(Ui.Page pojo) {
    title = Objects.requireNonNull(pojo.title, "title == null");

    body = Objects.requireNonNull(pojo.body, "body == null");
  }

  @Override
  protected final void render() {
    doctype();

    html(
        css("""
        background-color:html

        color:text
        """),

        lang("en"),

        head(
            meta(charset("utf-8")),
            meta(httpEquiv("content-type"), content("text/html; charset=utf-8")),
            meta(name("viewport"), content("width=device-width, initial-scale=1")),
            link(rel("stylesheet"), type("text/css"), href("/styles.css")),
            script(src("/script.js")),
            title(title)
        ),

        body(
            css("""
            width:100%
            min-height:100dvh

            background-color:body
            """),

            renderComponent(body)
        )
    );
  }

}
