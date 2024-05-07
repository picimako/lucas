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

package com.picimako.org.apache.lucene.luke.app.desktop;

import static com.picimako.org.apache.lucene.luke.app.desktop.util.ExceptionHandler.handle;

import com.intellij.openapi.project.Project;
import com.picimako.org.apache.lucene.luke.app.desktop.components.LukeWindowProvider;
import com.picimako.org.apache.lucene.luke.util.LucasLoggerFactory;

import javax.swing.*;
import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry class for desktop Luke
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.LukeMain}.
 */
public final class LukeMain {

  static {
    LucasLoggerFactory.initGuiLogging();
  }

  private static final Logger log = LucasLoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static JPanel frame;

  public static JPanel getOwnerFrame() {
    return frame;
  }

  public static boolean createGUI(Project project) {
    // uncaught error handler
    MessageBroker messageBroker = MessageBroker.getInstance();
    try {
      Thread.setDefaultUncaughtExceptionHandler((thread, cause) -> handle(cause, messageBroker));

      frame = new LukeWindowProvider(project).get();
      frame.setVisible(true);
      return true;
    } catch (Throwable e) {
      messageBroker.showUnknownErrorMessage();
      log.log(Level.SEVERE, "Cannot initialize components.", e);
      return false;
    }
  }

  public static void clear() {
    frame = null;
  }

  private LukeMain() {
  }
}
