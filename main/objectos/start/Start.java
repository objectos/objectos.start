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

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import objectos.start.app.Project;
import objectos.start.app.Site;
import objectos.way.App;
import objectos.way.Http;
import objectos.way.Lang;
import objectos.way.Note;
import objectos.way.Script;
import objectos.way.Web;

public final class Start extends App.Bootstrap {

  // We introduce this indirection so options can use the bootOptions map
  private class Options {

    final Option<Integer> port = optionInteger(opt -> {
      opt.name("--port");
      opt.value(4000);
    });

    final Option<Path> projectFile = optionPath(opt -> {
      opt.name("--project-file");
      opt.value(Start.this.<Path> bootOption("--workdir").resolve("project.toml"));
    });

  }

  private enum Stage {

    DEV,

    PROD;

  }

  public static final Lang.Key<Path> STYLES_SCAN_DIRECTORY = Lang.Key.of("STYLES_SCAN_DIRECTORY");

  private final Map<String, Object> bootOptions;

  private final Path classOutput = Path.of("work", "main");

  private final Options options;

  public Start(Map<String, Object> bootOptions) {
    this.bootOptions = bootOptions;

    options = new Options();
  }

  @Override
  protected final void bootstrap() {
    // Mark bootstrap start time
    final long startTime;
    startTime = System.currentTimeMillis();

    // App.Injector
    final App.Injector injector;
    injector = App.Injector.create(this::injector);

    final Note.Sink noteSink;
    noteSink = injector.getInstance(Note.Sink.class);

    // Http.Server
    final Http.Server server;
    server = Http.Server.create(opts -> {
      opts.bufferSize(8192, 8192);

      final Http.Handler handler;
      handler = serverHandler(injector);

      opts.handler(handler);

      opts.noteSink(noteSink);

      opts.port(options.port.get());
    });

    final App.ShutdownHook shutdownHook;
    shutdownHook = injector.getInstance(App.ShutdownHook.class);

    shutdownHook.register(server);

    try {
      server.start();
    } catch (IOException e) {
      throw App.serviceFailed("Http.Server", e);
    }

    // Note the bootstrap total time
    final Note.Long1 totalTimeNote;
    totalTimeNote = Note.Long1.create(getClass(), "TMS", Note.INFO);

    final long totalTime;
    totalTime = System.currentTimeMillis() - startTime;

    noteSink.send(totalTimeNote, totalTime);
  }

  private void injector(App.Injector.Options ctx) {
    // Note.Sink
    final Note.Sink noteSink;
    noteSink = noteSink();

    ctx.putInstance(Note.Sink.class, noteSink);

    // bootstrap start event
    final Note.Ref0 startNote;
    startNote = Note.Ref0.create(getClass(), "STA", Note.INFO);

    noteSink.send(startNote);

    // App.ShutdownHook
    final App.ShutdownHook shutdownHook;
    shutdownHook = App.ShutdownHook.create(config -> config.noteSink(noteSink));

    ctx.putInstance(App.ShutdownHook.class, shutdownHook);

    shutdownHook.registerIfPossible(noteSink);

    // Web.Resources
    final Web.Resources webResources;
    webResources = webResources(ctx);

    ctx.putInstance(Web.Resources.class, webResources);

    shutdownHook.register(webResources);

    // Project Model
    final Path projectFile;
    projectFile = options.projectFile.get();

    final Project.Model model;
    model = Project.Model.load(projectFile);

    ctx.putInstance(Project.Model.class, model);

    // Stage
    switch (stage()) {
      case DEV -> {
        ctx.putInstance(STYLES_SCAN_DIRECTORY, classOutput);
      }

      case PROD -> {

      }
    }
  }

  private Note.Sink noteSink() {
    final Appendable logger;
    logger = bootOption("logger");

    return App.NoteSink.ofAppendable(logger);
  }

  private Web.Resources webResources(App.Injector ctx) {
    try {
      return Web.Resources.create(opts -> {
        final Note.Sink noteSink;
        noteSink = ctx.getInstance(Note.Sink.class);

        opts.noteSink(noteSink);

        opts.contentTypes("""
        .css: text/css; charset=utf-8
        """);

        opts.addMedia("/script.js", Script.Library.of());
      });
    } catch (IOException e) {
      throw App.serviceFailed("Web.Resources", e);
    }
  }

  private record Reloader(App.Injector injector) implements App.Reloader.HandlerFactory {
    @Override
    public final Http.Handler reload(ClassLoader loader) throws Exception {
      final Class<? extends Reloader> self;
      self = getClass();

      final Module original;
      original = self.getModule();

      final Class<?> reloadedClass;
      reloadedClass = loader.loadClass("objectos.start.Dev");

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

  private Http.Handler serverHandler(App.Injector injector) {
    return switch (stage()) {
      case DEV -> {
        try {
          yield App.Reloader.create(opts -> {
            opts.handlerFactory(new Reloader(injector));

            opts.moduleOf(Start.class);

            final Note.Sink noteSink;
            noteSink = injector.getInstance(Note.Sink.class);

            opts.noteSink(noteSink);

            opts.directory(classOutput);
          });
        } catch (IOException e) {
          throw App.serviceFailed("App.Reloader", e);
        }
      }

      case PROD -> {
        final Http.Routing.Module module;
        module = new Site(injector);

        yield Http.Handler.of(module);
      }
    };
  }

  @SuppressWarnings("unchecked")
  private <T> T bootOption(String name) {
    final Object option;
    option = bootOptions.get(name);

    if (option == null) {
      throw new NoSuchElementException(name);
    }

    return (T) option;
  }

  private Stage stage() {
    final String stageName;
    stageName = bootOption("--stage");

    final String upper;
    upper = stageName.toUpperCase(Locale.US);

    return Stage.valueOf(upper);
  }

}