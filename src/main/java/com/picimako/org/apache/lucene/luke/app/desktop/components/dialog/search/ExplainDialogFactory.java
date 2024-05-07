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

package com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.search;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.treeStructure.Tree;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.search.Explanation;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Factory of explain dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.search.ExplainDialogFactory}.
 */
public final class ExplainDialogFactory extends DialogWrapper {

  private final int docid;

  private final Explanation explanation;

  public ExplainDialogFactory(@Nullable Project project, int docid, Explanation explanation) {
    super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);

    if (docid < 0 || Objects.isNull(explanation)) {
      throw new IllegalStateException("docid and/or explanation is not set.");
    }

    this.docid = docid;
    this.explanation = explanation;

    setTitle("Explanation");
    setSize(600, 400);

    setOKButtonText(MessageUtils.getLocalizedMessage("button.copy"));
    setOKButtonIcon(AllIcons.Actions.Copy);
    setCancelButtonText(MessageUtils.getLocalizedMessage("button.close"));

    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return content();
  }

  @Override
  protected void doOKAction() {
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(explanationToString());
    clipboard.setContents(selection, null);
  }

  private JPanel content() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 10));
    header.setOpaque(false);
    header.add(new JLabel(MessageUtils.getLocalizedMessage("search.explanation.description")));
    header.add(new JLabel(String.valueOf(docid)));
    panel.add(header, BorderLayout.PAGE_START);

    JPanel center = new JPanel(new GridLayout(1, 1));
    center.setOpaque(false);
    center.add(new JScrollPane(createExplanationTree()));
    panel.add(center, BorderLayout.CENTER);

    return panel;
  }

  private JTree createExplanationTree() {
    DefaultMutableTreeNode top = createNode(explanation);
    traverse(top, explanation.getDetails());

    JTree tree = new Tree(top);
    tree.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
    renderer.setOpenIcon(null);
    renderer.setClosedIcon(null);
    renderer.setLeafIcon(null);
    tree.setCellRenderer(renderer);
    // expand all nodes
    for (int row = 0; row < tree.getRowCount(); row++) {
      tree.expandRow(row);
    }
    return tree;
  }

  private void traverse(DefaultMutableTreeNode parent, Explanation[] explanations) {
    for (Explanation explanation : explanations) {
      DefaultMutableTreeNode node = createNode(explanation);
      parent.add(node);
      traverse(node, explanation.getDetails());
    }
  }

  private DefaultMutableTreeNode createNode(Explanation explanation) {
    return new DefaultMutableTreeNode(format(explanation));
  }

  private String explanationToString() {
    StringBuilder sb = new StringBuilder(format(explanation));
    sb.append(System.lineSeparator());
    traverseToCopy(sb, 1, explanation.getDetails());
    return sb.toString();
  }

  private void traverseToCopy(StringBuilder sb, int depth, Explanation[] explanations) {
    for (Explanation explanation : explanations) {
      IntStream.range(0, depth).forEach(i -> sb.append("  "));
      sb.append(format(explanation));
      sb.append("\n");
      traverseToCopy(sb, depth + 1, explanation.getDetails());
    }
  }

  private String format(Explanation explanation) {
    return explanation.getValue() + " " + explanation.getDescription();
  }
}
