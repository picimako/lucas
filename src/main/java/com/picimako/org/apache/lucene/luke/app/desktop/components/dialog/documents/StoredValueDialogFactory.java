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

package com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.documents;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

/**
 * Factory of stored values dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.documents.StoredValueDialogFactory}.
 */
public final class StoredValueDialogFactory extends DialogWrapper {

  private final String field;

  private final String value;

  public StoredValueDialogFactory(@Nullable Project project, String field, String value) {
    super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);

    if (Objects.isNull(field) || Objects.isNull(value)) {
      throw new IllegalStateException("field name and/or stored value is not set.");
    }

    this.field = field;
    this.value = value;

    setTitle("Term Vector");
    setSize(400, 300);
    setOKButtonText(MessageUtils.getLocalizedMessage("button.copy"));
    setOKButtonIcon(AllIcons.Actions.Copy);
    setCancelButtonText(MessageUtils.getLocalizedMessage("button.close"));

    init();
  }

  @Override
  protected void doOKAction() {
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(value);
    clipboard.setContents(selection, null);
    super.doOKAction();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return content();
  }

  private JPanel content() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
    header.setOpaque(false);
    header.add(new JLabel(MessageUtils.getLocalizedMessage("documents.stored.label.stored_value")));
    header.add(new JLabel(field));
    panel.add(header, BorderLayout.PAGE_START);

    JTextArea valueTA = new JBTextArea(value);
    valueTA.setLineWrap(true);
    valueTA.setEditable(false);
//    valueTA.setBackground(Color.white);
    JScrollPane scrollPane = new JBScrollPane(valueTA);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }
}
