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
import java.util.Map;
import java.util.NoSuchElementException;
import objectos.way.App;
import objectos.way.Http;
import objectos.way.Note;

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

      final Ui.Module module;
      module = new Ui.Module();

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

    // ShutdownHook
    final App.ShutdownHook shutdownHook;
    shutdownHook = App.ShutdownHook.create(config -> config.noteSink(noteSink));

    ctx.putInstance(App.ShutdownHook.class, shutdownHook);

    shutdownHook.registerIfPossible(noteSink);

    // Project Model
    final Path projectFile;
    projectFile = options.projectFile.get();

    final Project.Model model;
    model = Project.Model.load(projectFile);

    ctx.putInstance(Project.Model.class, model);
  }

  private Note.Sink noteSink() {
    final Appendable logger;
    logger = bootOption("logger");

    return App.NoteSink.ofAppendable(logger);
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