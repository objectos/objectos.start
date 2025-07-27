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

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.WaitForURLOptions;
import java.util.concurrent.TimeUnit;

final class YTab implements Y.Tab {

  private final String baseUrl;

  private final Page page;

  YTab(String baseUrl, Page page) {
    this.baseUrl = baseUrl;

    this.page = page;
  }

  @Override
  public final void dev() {
    final boolean headless;
    headless = Boolean.getBoolean("playwright.headless");

    if (headless) {
      // noop if running in headless mode
      return;
    }

    final WaitForURLOptions options;
    options = new Page.WaitForURLOptions().setTimeout(TimeUnit.DAYS.toMillis(1));

    page.waitForURL(baseUrl + "/dev-stop", options);
  }

  @Override
  public final void navigate(String path) {
    if (path.isEmpty()) {
      throw new IllegalArgumentException("path is empty");
    }

    final char first;
    first = path.charAt(0);

    if (first != '/') {
      throw new IllegalArgumentException("path must start with the '/' character");
    }

    final String url;
    url = baseUrl + path;

    page.navigate(url);
  }

  @Override
  public final String title() {
    return page.title();
  }

}