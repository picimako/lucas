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

package com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.menubar;

import com.intellij.openapi.project.Project;
import com.picimako.org.apache.lucene.luke.app.DirectoryHandler;
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.DialogFactory;
import org.apache.lucene.luke.app.DirectoryObserver;
import org.apache.lucene.luke.app.IndexObserver;
import org.apache.lucene.luke.app.LukeState;
import org.apache.lucene.luke.models.tools.IndexTools;
import org.apache.lucene.luke.models.tools.IndexToolsFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory of check index dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.menubar.CheckIndexDialogFactory}.
 */
public final class CheckIndexDialogFactory implements DialogFactory<CheckIndexDialog> {

  private final IndexToolsFactory indexToolsFactory;

  private final DirectoryHandler directoryHandler;

  private final IndexHandler indexHandler;

  private LukeState lukeState;

  private IndexTools toolsModel;

  public CheckIndexDialogFactory() {
    this.indexToolsFactory = new IndexToolsFactory();
    this.indexHandler = IndexHandler.getInstance();
    this.directoryHandler = DirectoryHandler.getInstance();

    indexHandler.addObserver(new Observer());
    directoryHandler.addObserver(new Observer());
  }

  @Override
  public CheckIndexDialog createDialog(@NotNull Project project) {
    return new CheckIndexDialog(project, toolsModel, lukeState);
  }

  private class Observer implements IndexObserver, DirectoryObserver {

    @Override
    public void openIndex(LukeState state) {
      lukeState = state;
      toolsModel =
          indexToolsFactory.newInstance(
              state.getIndexReader(), state.useCompound(), state.keepAllCommits());
    }

    @Override
    public void closeIndex() {
      close();
    }

    @Override
    public void openDirectory(LukeState state) {
      lukeState = state;
      toolsModel = indexToolsFactory.newInstance(state.getDirectory());
    }

    @Override
    public void closeDirectory() {
      close();
    }

    private void close() {
      toolsModel = null;
    }
  }
}
