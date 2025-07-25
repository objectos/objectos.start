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

import java.nio.file.Path;
import objectos.way.App;
import objectos.way.Css;
import objectos.way.Html;
import objectos.way.Note;

final class Ui {

  @Css.Source
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
              link(rel("stylesheet"), type("text/css"), href("/styles.css")),
              script(src("/script.js")),
              title(title)
          ),
          body(
          )
      );
    }
  }

  static Css.StyleSheet styles(App.Injector injector) {
    return Css.StyleSheet.create(opts -> {
      final Note.Sink noteSink;
      noteSink = injector.getInstance(Note.Sink.class);

      opts.noteSink(noteSink);

      final Path scanDirectory;
      scanDirectory = injector.getInstance(Start.STYLES_SCAN_DIRECTORY);

      opts.scanDirectory(scanDirectory);

      opts.theme("""
      --font-sans: 'InterVariable', var(--default-font-sans);
      --font-mono: 'Hack', var(--default-font-mono);
      --color-body: var(--color-white);
      --color-border: var(--color-gray-200);
      --color-border-strong: var(--color-gray-600);
      --color-btn-ghost: var(--color-body);
      --color-btn-ghost-active: color-mix(in oklab, var(--color-btn-ghost) 85%, black 15%);
      --color-btn-ghost-hover: color-mix(in oklab, var(--color-btn-ghost) 90%, black 10%);
      --color-btn-ghost-text: var(--color-text);
      --color-btn-primary: var(--color-blue-600);
      --color-btn-primary-active: color-mix(in oklab, var(--color-btn-primary) 70%, black 30%);
      --color-btn-primary-hover: color-mix(in oklab, var(--color-btn-primary) 85%, black 15%);
      --color-btn-primary-text: var(--color-gray-50);
      --color-field: var(--color-gray-100);
      --color-focus: var(--color-blue-600);
      --color-footer: var(--color-gray-700);
      --color-footer-text: var(--color-gray-100);
      --color-high-comment: var(--color-gray-500);
      --color-high-keyword: var(--color-blue-700);
      --color-high-literal: var(--color-red-600);
      --color-high-meta: var(--color-yellow-600);
      --color-high-string: var(--color-green-700);
      --color-html: var(--color-gray-50);
      --color-icon: var(--color-gray-800);
      --color-layer: var(--color-stone-100);
      --color-link: var(--color-blue-600);
      --color-link-hover: color-mix(in oklab, var(--color-link) 85%, black 15%);
      --color-logo: var(--color-gray-800);
      --color-logo-hover: var(--color-link);
      --color-text: var(--color-gray-800);
      --color-text-secondary: var(--color-gray-600);
      """);

      opts.theme("@media (prefers-color-scheme: dark)", """
      --color-body: var(--color-neutral-800);
      --color-border: var(--color-neutral-600);
      --color-btn-ghost-active: color-mix(in oklab, var(--color-btn-ghost) 85%, white 15%);
      --color-btn-ghost-hover: color-mix(in oklab, var(--color-btn-ghost) 90%, white 10%);
      --color-focus: var(--color-white);
      --color-high-comment: var(--color-gray-400);
      --color-high-keyword: var(--color-blue-400);
      --color-high-literal: var(--color-red-400);
      --color-high-meta: var(--color-yellow-500);
      --color-high-string: var(--color-green-400);
      --color-icon: var(--color-gray-200);
      --color-layer: var(--color-stone-900);
      --color-link: var(--color-blue-400);
      --color-link-hover: color-mix(in oklab, var(--color-link) 85%, white 15%);
      --color-logo: var(--color-neutral-100);
      --color-text: var(--color-neutral-100);
      --color-text-secondary: var(--color-neutral-300);
      """);
    });
  }

}