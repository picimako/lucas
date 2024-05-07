/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.picimako.org.apache.lucene.luke.util;

import org.apache.lucene.luke.util.LoggerFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logger factory. This configures log interceptors for the GUI.
 * <p>
 * LICENSE NOTE: This class is based on {@link LoggerFactory}, and
 * <ul>
 *     <li>it enables the logging of entries coming from this plugin, by the modified Luke classes, in
 *     the {@code com.picimako.org.apache.lucene} hierarchy,</li>
 *     <li>it also enables the logging of entries in the {@code org.apache.lucene} hierarchy by also
 *     initializing {@code LoggerFactory}.</li>
 * </ul>
 * It uses the same {@link org.apache.lucene.luke.util.CircularLogBufferHandler} instance (the one in {@code LoggerFactory}),
 * so that the Logs tab can use the same central buffer as source.
 */
public final class LucasLoggerFactory {
  public static void initGuiLogging() {
    LoggerFactory.initGuiLogging();

    // Only capture events from Lucene logger hierarchy from this plugin.
    var luceneRoot = Logger.getLogger("com.picimako.org.apache.lucene");
    luceneRoot.setLevel(Level.FINEST);
    luceneRoot.addHandler(LoggerFactory.circularBuffer);
  }

  public static Logger getLogger(Class<?> clazz) {
    return Logger.getLogger(clazz.getName());
  }
}
