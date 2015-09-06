/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.rolling.helper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class AsynchronousCompressor {
  Compressor compressor;

  public AsynchronousCompressor(Compressor compressor) {
    this.compressor = compressor;
  }

  public Future<?> compressAsynchronously(String nameOfFile2Compress,
      String nameOfCompressedFile, String innerEntryName, final ICallback callback) {
    ExecutorService executor = getExecutor(callback);
    Future<?> future = executor.submit(new CompressionRunnable(compressor,
        nameOfFile2Compress, nameOfCompressedFile, innerEntryName));
    executor.shutdown();
    return future;
  }

  private static ExecutorService getExecutor(final ICallback callback) {
    final AtomicLong count = new AtomicLong(0);
    return Executors.newScheduledThreadPool(1, new ThreadFactory() {
      @Override
      public Thread newThread(final Runnable r) {
        Thread t = new Thread() {
          public void run() {
            try {
              r.run();
            } finally {
              if (callback != null) {
                callback.done();
              }
            }
          }
        };
        t.setDaemon(false);
        t.setName("logback-file-compressor" + count.getAndIncrement());
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
      }
    });
  }

  public interface ICallback {
    void done();
  }
}
