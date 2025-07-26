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