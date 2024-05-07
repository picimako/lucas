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
import com.intellij.ui.JBColor;
import com.picimako.org.apache.lucene.luke.app.DirectoryHandler;
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.MessageBroker;
import org.apache.lucene.luke.app.DirectoryObserver;
import org.apache.lucene.luke.app.IndexObserver;
import org.apache.lucene.luke.app.LukeState;
import org.apache.lucene.luke.app.desktop.util.ImageUtils;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Provider of the root window
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.LukeWindowProvider}.
 */
public final class LukeWindowProvider {

  private final MessageBroker messageBroker;

  private final TabSwitcherProxy tabSwitcher;

  private final JMenuBar menuBar;

  private final JTabbedPane tabbedPane;

  private final JLabel messageLbl = new JLabel();

  private final JLabel multiIcon = new JLabel(AllIcons.Actions.GroupBy);

  private final JLabel readOnlyIcon = new JLabel(AllIcons.Nodes.Padlock);

  private final JLabel noReaderIcon = new JLabel(AllIcons.General.ReaderMode);

  public LukeWindowProvider(Project project) {
    this.menuBar = new MenuBarProvider(project).get();
    this.tabbedPane = new TabbedPaneProvider(project).get();
    this.messageBroker = MessageBroker.getInstance();
    this.tabSwitcher = TabSwitcherProxy.getInstance();

    Observer observer = new Observer();
    DirectoryHandler.getInstance().addObserver(observer);
    IndexHandler.getInstance().addObserver(observer);

    messageBroker.registerReceiver(new MessageReceiverImpl());
  }

  public JPanel get() {
    JPanel frame = new JPanel();
    frame.setLayout(new BoxLayout(frame, BoxLayout.PAGE_AXIS));
    frame.add(menuBar);
    frame.add(initMainPanel());
    frame.add(initMessagePanel());
    return frame;
  }

  private JPanel initMainPanel() {
    JPanel panel = new JPanel(new GridLayout(1, 1));

    tabbedPane.setEnabledAt(TabbedPaneProvider.Tab.OVERVIEW.index(), false);
    tabbedPane.setEnabledAt(TabbedPaneProvider.Tab.DOCUMENTS.index(), false);
    tabbedPane.setEnabledAt(TabbedPaneProvider.Tab.SEARCH.index(), false);
    tabbedPane.setEnabledAt(TabbedPaneProvider.Tab.COMMITS.index(), false);

    panel.add(tabbedPane);

    panel.setOpaque(false);
    return panel;
  }

  private JPanel initMessagePanel() {
    JPanel panel = new JPanel(new GridLayout(1, 1));
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));

    JPanel innerPanel = new JPanel(new GridBagLayout());
    innerPanel.setOpaque(false);
    innerPanel.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;

    JPanel msgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    msgPanel.setOpaque(false);
    msgPanel.add(messageLbl);

    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0.8;
    innerPanel.add(msgPanel, c);

    JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    iconPanel.setOpaque(false);

    multiIcon.setToolTipText(MessageUtils.getLocalizedMessage("tooltip.multi_reader"));
    multiIcon.setVisible(false);
    iconPanel.add(multiIcon);

    readOnlyIcon.setToolTipText(MessageUtils.getLocalizedMessage("tooltip.read_only"));
    readOnlyIcon.setVisible(false);
    iconPanel.add(readOnlyIcon);

    noReaderIcon.setToolTipText(MessageUtils.getLocalizedMessage("tooltip.no_reader"));
    noReaderIcon.setVisible(false);
    iconPanel.add(noReaderIcon);

    JLabel luceneIcon = new JLabel(ImageUtils.createImageIcon("lucene.gif", "lucene", 16, 16));
    iconPanel.add(luceneIcon);

    c.gridx = 1;
    c.gridy = 0;
    c.weightx = 0.2;
    innerPanel.add(iconPanel);
    panel.add(innerPanel);

    return panel;
  }

  private class Observer implements IndexObserver, DirectoryObserver {

    @Override
    public void openDirectory(LukeState state) {
      multiIcon.setVisible(false);
      readOnlyIcon.setVisible(false);
      noReaderIcon.setVisible(true);

      tabSwitcher.switchTab(TabbedPaneProvider.Tab.COMMITS);

      messageBroker.showStatusMessage(MessageUtils.getLocalizedMessage("message.directory_opened"));
    }

    @Override
    public void closeDirectory() {
      multiIcon.setVisible(false);
      readOnlyIcon.setVisible(false);
      noReaderIcon.setVisible(false);

      messageBroker.showStatusMessage(MessageUtils.getLocalizedMessage("message.directory_closed"));
    }

    @Override
    public void openIndex(LukeState state) {
      multiIcon.setVisible(!state.hasDirectoryReader());
      readOnlyIcon.setVisible(state.readOnly());
      noReaderIcon.setVisible(false);

      if (state.readOnly()) {
        messageBroker.showStatusMessage(
            MessageUtils.getLocalizedMessage("message.index_opened_ro"));
      } else if (!state.hasDirectoryReader()) {
        messageBroker.showStatusMessage(
            MessageUtils.getLocalizedMessage("message.index_opened_multi"));
      } else {
        messageBroker.showStatusMessage(MessageUtils.getLocalizedMessage("message.index_opened"));
      }
    }

    @Override
    public void closeIndex() {
      multiIcon.setVisible(false);
      readOnlyIcon.setVisible(false);
      noReaderIcon.setVisible(false);

      messageBroker.showStatusMessage(MessageUtils.getLocalizedMessage("message.index_closed"));
    }
  }

  private class MessageReceiverImpl implements MessageBroker.MessageReceiver {

    @Override
    public void showStatusMessage(String message) {
      messageLbl.setText(message);
    }

    @Override
    public void showUnknownErrorMessage() {
      messageLbl.setText(MessageUtils.getLocalizedMessage("message.error.unknown"));
    }

    @Override
    public void clearStatusMessage() {
      messageLbl.setText("");
    }

    private MessageReceiverImpl() {}
  }
}
