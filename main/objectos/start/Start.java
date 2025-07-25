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
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
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

    final Option<String> stage = optionString(opt -> {
      opt.name("--stage");
      opt.validator(s -> s.equalsIgnoreCase("prod") || s.equalsIgnoreCase("dev"), "Allowed values: prod | dev");
      opt.value("prod");
    });

  }

  static final Lang.Key<Path> STYLES_SCAN_DIRECTORY = Lang.Key.of("STYLES_SCAN_DIRECTORY");

  private final Map<String, Object> bootOptions;

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

      final Http.Routing.Module module;
      module = new Site(injector);

      final Http.Handler handler;
      handler = Http.Handler.of(module);

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
    final String stage;
    stage = options.stage.get();

    switch (stage.toUpperCase(Locale.US)) {
      case "DEV" -> {
        final Path classOutput;
        classOutput = Path.of("work", "main");

        ctx.putInstance(STYLES_SCAN_DIRECTORY, classOutput);
      }

      case "PROD" -> {

      }

      default -> throw new AssertionError("Unexpected stage. Only prod or dev allowed.");
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

  @SuppressWarnings("unchecked")
  private <T> T bootOption(String name) {
    final Object option;
    option = bootOptions.get(name);

    if (option == null) {
      throw new NoSuchElementException(name);
    }

    return (T) option;
  }

}