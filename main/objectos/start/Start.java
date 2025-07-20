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
import java.util.Map;
import java.util.NoSuchElementException;
import objectos.way.App;
import objectos.way.Note;
import objectos.way.Sql;
import org.h2.jdbcx.JdbcConnectionPool;

public final class Start extends App.Bootstrap {

  // We introduce this indirection so options can use the bootOptions map more easily
  private class Options {

    final Option<Path> db = optionPath(opt -> {
      opt.name("--db");
      opt.required();
      opt.value(Start.this.<Path> bootOption("--workdir").resolve("db"));
    });

    final Option<Integer> dbPoolSize = optionInteger(opt -> {
      opt.name("--db-pool-size");
      opt.required();
      opt.value(1);
    });

    final Option<String> dbRoot = optionString(opt -> {
      opt.name("--db-root");
      opt.required();
      opt.value("root");
    });

    final Option<String> dbRootPassword = optionString(opt -> {
      opt.name("--db-root-password");
      opt.required();
      opt.value("");
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

    // Database
    final Sql.Database db;
    db = db(ctx);

    ctx.putInstance(Sql.Database.class, db);
  }

  private Note.Sink noteSink() {
    final Appendable logger;
    logger = bootOption("logger");

    return App.NoteSink.ofAppendable(logger);
  }

  private Sql.Database db(App.Injector ctx) {
    final Path dbPath;
    dbPath = options.db.get();

    final String url;
    url = "jdbc:h2:file:" + dbPath;

    final String root;
    root = options.dbRoot.get();

    final String rootPassword;
    rootPassword = options.dbRootPassword.get();

    final JdbcConnectionPool pool;
    pool = JdbcConnectionPool.create(url, root, rootPassword);

    final Integer poolSize;
    poolSize = options.dbPoolSize.get();

    pool.setMaxConnections(poolSize.intValue());

    final App.ShutdownHook shutdownHook;
    shutdownHook = ctx.getInstance(App.ShutdownHook.class);

    shutdownHook.register(pool::dispose);

    return Sql.Database.create(config -> {
      config.dataSource(pool);

      final Note.Sink noteSink;
      noteSink = ctx.getInstance(Note.Sink.class);

      config.noteSink(noteSink);
    });
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