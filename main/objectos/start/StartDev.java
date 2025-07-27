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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import objectos.start.app.Routes;
import objectos.start.app.Ui;
import objectos.way.App;
import objectos.way.Css;
import objectos.way.Http;
import objectos.way.Media;
import objectos.way.Note;

public final class StartDev extends Start {

  public StartDev(Map<String, Object> bootOptions) {
    super(bootOptions);
  }

  @Override
  final void injectorStage(App.Injector.Options ctx) {
    ctx.putInstance(STYLES_SCAN_DIRECTORY, bootOption("--class-output"));
  }

  private static final class ThisRoutes extends Routes {

    ThisRoutes(App.Injector injector) {
      super(injector);
    }

    @Override
    protected final void configureStage(Http.Routing routing) {
      routing.path("/styles.css", path -> {
        // in prod, we serve the file from the filesystem
        // in dev, we generate the file on each request
        path.allow(Http.Method.GET, this::styles);
      });

      routing.path("/dev-stop", path -> {
        path.allow(Http.Method.GET, http -> http.ok(Media.Bytes.textPlain("ok\n")));
      });
    }

    private void styles(Http.Exchange http) {
      final Css.StyleSheet styles;
      styles = Ui.styles(injector);

      http.ok(styles);
    }

  }

  public static Object reload(Object arg0, Module original) {
    final Module reloaded;
    reloaded = StartDev.class.getModule();

    reloaded.addReads(original);

    final App.Injector injector;
    injector = (App.Injector) arg0;

    return new ThisRoutes(injector);
  }

  private static final class Reloader implements App.Reloader.HandlerFactory {

    private final App.Injector injector;

    private Reloader(App.Injector injector) {
      this.injector = injector;
    }

    @Override
    public final Http.Handler reload(ClassLoader loader) throws Exception {
      final Class<? extends Reloader> self;
      self = getClass();

      final Module original;
      original = self.getModule();

      final Class<?> reloadedClass;
      reloadedClass = loader.loadClass("objectos.start.StartDev");

      final Module reloaded;
      reloaded = reloadedClass.getModule();

      original.addReads(reloaded);

      original.addExports("objectos.start.app", reloaded);

      final Method reloadMethod;
      reloadMethod = reloadedClass.getMethod("reload", Object.class, Module.class);

      final Object instance;
      instance = reloadMethod.invoke(null, injector, original);

      final Http.Routing.Module module;
      module = (Http.Routing.Module) instance;

      return Http.Handler.of(module);
    }

  }

  @Override
  final Http.Handler serverHandler(App.Injector injector) {
    try {
      return App.Reloader.create(opts -> {
        opts.handlerFactory(new Reloader(injector));

        opts.moduleOf(Start.class);

        final Note.Sink noteSink;
        noteSink = injector.getInstance(Note.Sink.class);

        opts.noteSink(noteSink);

        opts.directory(bootOption("--class-output"));
      });
    } catch (IOException e) {
      throw App.serviceFailed("App.Reloader", e);
    }
  }

}
