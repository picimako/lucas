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

package com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.analysis;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import org.apache.lucene.analysis.CharFilterFactory;
import org.apache.lucene.analysis.TokenFilterFactory;
import org.apache.lucene.analysis.TokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Factory of analysis chain dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.analysis.AnalysisChainDialogFactory}.
 */
public class AnalysisChainDialogFactory extends DialogWrapper {

  private final CustomAnalyzer analyzer;

  public AnalysisChainDialogFactory(@Nullable Project project, CustomAnalyzer analyzer) {
    super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);
    this.analyzer = analyzer;

    setTitle("Analysis chain");
    setSize(600, 320);
    setCancelButtonText(MessageUtils.getLocalizedMessage("button.close"));

    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return content();
  }

  private JPanel content() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    panel.add(analysisChain(), BorderLayout.PAGE_START);

    return panel;
  }

  private JPanel analysisChain() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);

    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.insets = JBUI.insets(5);

    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0.1;
    c.weighty = 0.5;
    panel.add(
        new JLabel(MessageUtils.getLocalizedMessage("analysis.dialog.chain.label.charfilters")), c);

    String[] charFilters =
        analyzer.getCharFilterFactories().stream()
            .map(f -> CharFilterFactory.findSPIName(f.getClass()))
            .toArray(String[]::new);
    JList<String> charFilterList = new JBList<>(charFilters);
    charFilterList.setVisibleRowCount(
        charFilters.length == 0 ? 1 : Math.min(charFilters.length, 5));
    c.gridx = 1;
    c.gridy = 0;
    c.weightx = 0.5;
    c.weighty = 0.5;
    panel.add(new JBScrollPane(charFilterList), c);

    c.gridx = 0;
    c.gridy = 1;
    c.weightx = 0.1;
    c.weighty = 0.1;
    panel.add(
        new JLabel(MessageUtils.getLocalizedMessage("analysis.dialog.chain.label.tokenizer")), c);

    String tokenizer = TokenizerFactory.findSPIName(analyzer.getTokenizerFactory().getClass());
    JTextField tokenizerTF = new JBTextField(tokenizer);
    tokenizerTF.setColumns(30);
    tokenizerTF.setEditable(false);
    tokenizerTF.setPreferredSize(new Dimension(300, 25));
    tokenizerTF.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
    c.gridx = 1;
    c.gridy = 1;
    c.weightx = 0.5;
    c.weighty = 0.1;
    panel.add(tokenizerTF, c);

    c.gridx = 0;
    c.gridy = 2;
    c.weightx = 0.1;
    c.weighty = 0.5;
    panel.add(
        new JLabel(MessageUtils.getLocalizedMessage("analysis.dialog.chain.label.tokenfilters")),
        c);

    String[] tokenFilters =
        analyzer.getTokenFilterFactories().stream()
            .map(f -> TokenFilterFactory.findSPIName(f.getClass()))
            .toArray(String[]::new);
    JList<String> tokenFilterList = new JBList<>(tokenFilters);
    tokenFilterList.setVisibleRowCount(
        tokenFilters.length == 0 ? 1 : Math.min(tokenFilters.length, 5));
    tokenFilterList.setMinimumSize(new Dimension(300, 25));
    c.gridx = 1;
    c.gridy = 2;
    c.weightx = 0.5;
    c.weighty = 0.5;
    panel.add(new JScrollPane(tokenFilterList), c);

    return panel;
  }
}
