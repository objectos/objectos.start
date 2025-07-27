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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;
import objectos.start.app.Project;
import objectos.way.App;
import objectos.way.Http;
import objectos.way.Lang;
import objectos.way.Note;
import objectos.way.Script;
import objectos.way.Web;

public abstract class Start extends App.Bootstrap implements Closeable {

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

  public static final Lang.Key<Path> STYLES_SCAN_DIRECTORY = Lang.Key.of("STYLES_SCAN_DIRECTORY");

  private final Map<String, Object> bootOptions;

  private final Options options;

  private Closeable server;

  Start(Map<String, Object> bootOptions) {
    this.bootOptions = bootOptions;

    options = new Options();
  }

  @Override
  public final void close() throws IOException {
    if (server != null) {
      server.close();
    }
  }

  @Override
  protected final void bootstrap() {
    // Mark bootstrap start time
    final long startTime;
    startTime = System.currentTimeMillis();

    // App.Injector
    final App.Injector injector;
    injector = App.Injector.create(this::injector);

    // Http.Server
    server = server(injector);

    final App.ShutdownHook shutdownHook;
    shutdownHook = injector.getInstance(App.ShutdownHook.class);

    shutdownHook.register(server);

    // Note the bootstrap total time
    final Note.Long1 totalTimeNote;
    totalTimeNote = Note.Long1.create(getClass(), "TMS", Note.INFO);

    final long totalTime;
    totalTime = System.currentTimeMillis() - startTime;

    final Note.Sink noteSink;
    noteSink = injector.getInstance(Note.Sink.class);

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

    injectorStage(ctx);
  }

  abstract void injectorStage(App.Injector.Options ctx);

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

  abstract Http.Handler serverHandler(App.Injector injector);

  Closeable server(App.Injector injector) {
    try {
      final Http.Server server;
      server = Http.Server.create(opts -> {
        opts.bufferSize(8192, 8192);

        final Http.Handler handler;
        handler = serverHandler(injector);

        opts.handler(handler);

        final Note.Sink noteSink;
        noteSink = injector.getInstance(Note.Sink.class);

        opts.noteSink(noteSink);

        opts.port(options.port.get());
      });

      server.start();

      return server;
    } catch (IOException e) {
      throw App.serviceFailed("Http.Server", e);
    }
  }

  @SuppressWarnings("unchecked")
  final <T> T bootOption(String name) {
    final Object option;
    option = bootOptions.get(name);

    if (option == null) {
      throw new NoSuchElementException(name);
    }

    return (T) option;
  }

}