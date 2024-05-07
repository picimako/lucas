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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;
import com.picimako.org.apache.lucene.luke.app.DirectoryHandler;
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.MessageBroker;
import org.apache.lucene.luke.app.DirectoryObserver;
import org.apache.lucene.luke.app.IndexObserver;
import org.apache.lucene.luke.app.LukeState;

import javax.swing.*;

/**
 * Provider of the Tabbed pane
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.TabbedPaneProvider}.
 */
public final class TabbedPaneProvider implements TabSwitcherProxy.TabSwitcher {

  private final MessageBroker messageBroker;

  private final JTabbedPane tabbedPane;

  private final JPanel overviewPanel;

  private final JPanel documentsPanel;

  private final JPanel searchPanel;

  private final JPanel analysisPanel;

  private final JPanel commitsPanel;

  private final JPanel logsPanel;

  public TabbedPaneProvider(Project project) {
    this.tabbedPane = new JBTabbedPane();

    this.overviewPanel = new OverviewPanelProvider().get();
    this.documentsPanel = new DocumentsPanelProvider(project).get();
    this.searchPanel = new SearchPanelProvider(project).get();
    this.analysisPanel = new AnalysisPanelProvider(project).get();
    this.commitsPanel = new CommitsPanelProvider().get();
    this.logsPanel = new LogsPanelProvider().get();

    this.messageBroker = MessageBroker.getInstance();

    TabSwitcherProxy.getInstance().set(this);

    Observer observer = new Observer();
    IndexHandler.getInstance().addObserver(observer);
    DirectoryHandler.getInstance().addObserver(observer);
  }

  public JTabbedPane get() {
    tabbedPane.addTab("Overview", AllIcons.Nodes.HomeFolder, overviewPanel);
    tabbedPane.addTab("Documents", AllIcons.Actions.Copy, documentsPanel);
    tabbedPane.addTab("Search", AllIcons.Actions.Find, searchPanel);
    tabbedPane.addTab("Analysis", AllIcons.Actions.Edit, analysisPanel);
    tabbedPane.addTab("Commits", AllIcons.Actions.MenuSaveall, commitsPanel);
    tabbedPane.addTab("Logs", AllIcons.Actions.ListFiles, logsPanel);

    return tabbedPane;
  }

  @Override
  public void switchTab(Tab tab) {
    tabbedPane.setSelectedIndex(tab.index());
    tabbedPane.setVisible(false);
    tabbedPane.setVisible(true);
    messageBroker.clearStatusMessage();
  }

  private class Observer implements IndexObserver, DirectoryObserver {

    @Override
    public void openDirectory(LukeState state) {
      tabbedPane.setEnabledAt(Tab.COMMITS.index(), true);
    }

    @Override
    public void closeDirectory() {
      tabbedPane.setEnabledAt(Tab.OVERVIEW.index(), false);
      tabbedPane.setEnabledAt(Tab.DOCUMENTS.index(), false);
      tabbedPane.setEnabledAt(Tab.SEARCH.index(), false);
      tabbedPane.setEnabledAt(Tab.COMMITS.index(), false);
    }

    @Override
    public void openIndex(LukeState state) {
      tabbedPane.setEnabledAt(Tab.OVERVIEW.index(), true);
      tabbedPane.setEnabledAt(Tab.DOCUMENTS.index(), true);
      tabbedPane.setEnabledAt(Tab.SEARCH.index(), true);
      tabbedPane.setEnabledAt(Tab.COMMITS.index(), true);
    }

    @Override
    public void closeIndex() {
      tabbedPane.setEnabledAt(Tab.OVERVIEW.index(), false);
      tabbedPane.setEnabledAt(Tab.DOCUMENTS.index(), false);
      tabbedPane.setEnabledAt(Tab.SEARCH.index(), false);
      tabbedPane.setEnabledAt(Tab.COMMITS.index(), false);
    }
  }

  /** tabs in the main frame */
  public enum Tab {
    OVERVIEW(0),
    DOCUMENTS(1),
    SEARCH(2),
    ANALYZER(3),
    COMMITS(4);

    private int tabIdx;

    Tab(int tabIdx) {
      this.tabIdx = tabIdx;
    }

    int index() {
      return tabIdx;
    }
  }
}
