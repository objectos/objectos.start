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

import java.nio.file.Path;
import java.util.function.Consumer;
import objectos.start.Start;
import objectos.way.App;
import objectos.way.Css;
import objectos.way.Html;
import objectos.way.Media;
import objectos.way.Note;

public final class Ui {

  public static final class Page {

    public String title;

    public Html.Component body;

    private Page() {}

  }

  public static Media.Text page(Consumer<? super Page> opts) {
    final Page pojo;
    pojo = new Page();

    opts.accept(pojo);

    return new UiPage(pojo);
  }

  public static Css.StyleSheet styles(App.Injector injector) {
    return Css.StyleSheet.create(opts -> {
      final Note.Sink noteSink;
      noteSink = injector.getInstance(Note.Sink.class);

      opts.noteSink(noteSink);

      final Path scanDirectory;
      scanDirectory = injector.getInstance(Start.STYLES_SCAN_DIRECTORY);

      opts.scanDirectory(scanDirectory);

      opts.theme("""
      --color-body: var(--color-gray-100);
      --color-html: var(--color-gray-50);
      """);

      opts.theme("@media (prefers-color-scheme: dark)", """
      --color-body: var(--color-neutral-800);
      """);
    });
  }

}