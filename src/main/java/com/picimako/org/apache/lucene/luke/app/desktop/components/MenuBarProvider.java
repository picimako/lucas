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

package com.picimako.org.apache.lucene.luke.app.desktop.components;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.ui.components.JBMenu;
import com.picimako.org.apache.lucene.luke.app.DirectoryHandler;
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.menubar.CheckIndexDialogFactory;
import com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.menubar.CreateIndexDialogFactory;
import com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.menubar.ExportTermsDialogFactory;
import com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.menubar.OpenIndexDialogFactory;
import com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.menubar.OptimizeIndexDialogFactory;
import org.apache.lucene.luke.app.DirectoryObserver;
import org.apache.lucene.luke.app.IndexObserver;
import org.apache.lucene.luke.app.LukeState;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.models.LukeException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * Provider of the MenuBar
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.MenuBarProvider}.
 */
public final class MenuBarProvider {

  private final DirectoryHandler directoryHandler;

  private final IndexHandler indexHandler;

  private final OptimizeIndexDialogFactory optimizeIndexDialogFactory;

  private final ExportTermsDialogFactory exportTermsDialogFactory;

  private final CheckIndexDialogFactory checkIndexDialogFactory;

  private final JMenuItem openIndexMItem = new JBMenuItem(MessageUtils.getLocalizedMessage("menu.item.open_index"));

  private final JMenuItem reopenIndexMItem = new JBMenuItem(MessageUtils.getLocalizedMessage("menu.item.reopen_index"));

  private final JMenuItem createIndexMItem = new JBMenuItem(MessageUtils.getLocalizedMessage("menu.item.create_index"));

  private final JMenuItem closeIndexMItem = new JBMenuItem(MessageUtils.getLocalizedMessage("menu.item.close_index"));

  private final JMenuItem optimizeIndexMItem = new JBMenuItem(MessageUtils.getLocalizedMessage("menu.item.optimize"));

  private final JMenuItem exportTermsMItem = new JBMenuItem(MessageUtils.getLocalizedMessage("menu.item.export.terms"));

  private final JMenuItem checkIndexMItem = new JBMenuItem(MessageUtils.getLocalizedMessage("menu.item.check_index"));

  private final ListenerFunctions listeners = new ListenerFunctions();
  private final Project project;

  public MenuBarProvider(Project project) {
    this.project = project;
    this.directoryHandler = DirectoryHandler.getInstance();
    this.indexHandler = IndexHandler.getInstance();
    this.optimizeIndexDialogFactory = new OptimizeIndexDialogFactory();
    this.exportTermsDialogFactory = new ExportTermsDialogFactory();
    this.checkIndexDialogFactory = new CheckIndexDialogFactory();

    Observer observer = new Observer();
    directoryHandler.addObserver(observer);
    indexHandler.addObserver(observer);
  }

  public JMenuBar get() {
    JMenuBar menuBar = new JMenuBar();

    menuBar.add(createFileMenu());
    menuBar.add(createToolsMenu());

    return menuBar;
  }

  private JMenu createFileMenu() {
    JMenu fileMenu = new JBMenu();
    fileMenu.setText(MessageUtils.getLocalizedMessage("menu.file"));

    openIndexMItem.addActionListener(listeners::showOpenIndexDialog);
    fileMenu.add(openIndexMItem);

    reopenIndexMItem.setEnabled(false);
    reopenIndexMItem.addActionListener(listeners::reopenIndex);
    fileMenu.add(reopenIndexMItem);

    createIndexMItem.addActionListener(listeners::showCreateIndexDialog);
    fileMenu.add(createIndexMItem);

    closeIndexMItem.setEnabled(false);
    closeIndexMItem.addActionListener(listeners::closeIndex);
    fileMenu.add(closeIndexMItem);

    return fileMenu;
  }

  private JMenu createToolsMenu() {
    JMenu toolsMenu = new JMenu();
    toolsMenu.setText(MessageUtils.getLocalizedMessage("menu.tools"));
    optimizeIndexMItem.setEnabled(false);
    optimizeIndexMItem.addActionListener(listeners::showOptimizeIndexDialog);
    toolsMenu.add(optimizeIndexMItem);
    checkIndexMItem.setEnabled(false);
    checkIndexMItem.addActionListener(listeners::showCheckIndexDialog);
    toolsMenu.add(checkIndexMItem);
    exportTermsMItem.setEnabled(false);
    exportTermsMItem.addActionListener(listeners::showExportTermsDialog);
    toolsMenu.add(exportTermsMItem);
    return toolsMenu;
  }

  private class ListenerFunctions {

    void showOpenIndexDialog(ActionEvent e) {
      try {
        new OpenIndexDialogFactory(project).show();
      } catch (IOException ex) {
        throw new LukeException(ex);
      }
    }

    void showCreateIndexDialog(ActionEvent e) {
      try {
        new CreateIndexDialogFactory(project).show();
      } catch (IOException ex) {
        throw new LukeException(ex);
      }
    }

    void reopenIndex(ActionEvent e) {
      indexHandler.reOpen();
    }

    void closeIndex(ActionEvent e) {
      close();
    }

    private void close() {
      directoryHandler.close();
      indexHandler.close();
    }

    void showOptimizeIndexDialog(ActionEvent e) {
      optimizeIndexDialogFactory.createDialog(project).show();
    }

    void showCheckIndexDialog(ActionEvent e) {
      checkIndexDialogFactory.createDialog(project).show();
    }

    void showExportTermsDialog(ActionEvent e) {
      exportTermsDialogFactory.createDialog(project).show();
    }
  }

  private class Observer implements IndexObserver, DirectoryObserver {

    @Override
    public void openDirectory(LukeState state) {
      reopenIndexMItem.setEnabled(false);
      closeIndexMItem.setEnabled(false);
      optimizeIndexMItem.setEnabled(false);
      exportTermsMItem.setEnabled(false);
      checkIndexMItem.setEnabled(true);
    }

    @Override
    public void closeDirectory() {
      close();
    }

    @Override
    public void openIndex(LukeState state) {
      reopenIndexMItem.setEnabled(true);
      closeIndexMItem.setEnabled(true);
      exportTermsMItem.setEnabled(true);
      if (!state.readOnly() && state.hasDirectoryReader()) {
        optimizeIndexMItem.setEnabled(true);
      }
      if (state.hasDirectoryReader()) {
        checkIndexMItem.setEnabled(true);
      }
    }

    @Override
    public void closeIndex() {
      close();
    }

    private void close() {
      reopenIndexMItem.setEnabled(false);
      closeIndexMItem.setEnabled(false);
      optimizeIndexMItem.setEnabled(false);
      checkIndexMItem.setEnabled(false);
      exportTermsMItem.setEnabled(false);
    }
  }
}
