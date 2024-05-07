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
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.DialogFactory;
import org.apache.lucene.luke.app.IndexObserver;
import org.apache.lucene.luke.app.LukeState;
import org.apache.lucene.luke.models.tools.IndexTools;
import org.apache.lucene.luke.models.tools.IndexToolsFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory of optimize index dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.menubar.OptimizeIndexDialogFactory}.
 */
public final class OptimizeIndexDialogFactory implements DialogFactory<OptimizeIndexDialog> {

  private final IndexToolsFactory indexToolsFactory = new IndexToolsFactory();

  private final IndexHandler indexHandler;

  private IndexTools toolsModel;

  public OptimizeIndexDialogFactory() {
    this.indexHandler = IndexHandler.getInstance();
    indexHandler.addObserver(new Observer());
  }

  @Override
  public OptimizeIndexDialog createDialog(@NotNull Project project) {
    return new OptimizeIndexDialog(project, toolsModel);
  }

  private class Observer implements IndexObserver {

    @Override
    public void openIndex(LukeState state) {
      toolsModel =
          indexToolsFactory.newInstance(
              state.getIndexReader(), state.useCompound(), state.keepAllCommits());
    }

    @Override
    public void closeIndex() {
      toolsModel = null;
    }
  }
}
