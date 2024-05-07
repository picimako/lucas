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

package com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.picimako.org.apache.lucene.luke.app.desktop.components.ComponentOperatorRegistry;
import com.picimako.org.apache.lucene.luke.app.desktop.components.TabSwitcherProxy;
import com.picimako.org.apache.lucene.luke.app.desktop.components.TabbedPaneProvider;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.luke.app.desktop.util.FontUtils;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.models.analysis.AnalysisFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Provider of the Analyzer pane
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.fragments.search.AnalyzerPaneProvider}.
 */
public final class AnalyzerPaneProvider implements AnalyzerTabOperator {

  private final TabSwitcherProxy tabSwitcher;

  private final JLabel analyzerNameLbl = new JLabel();

  private final JList<String> charFilterList = new JBList<>();

  private final JTextField tokenizerTF = new JTextField();

  private final JList<String> tokenFilterList = new JBList<>();

  public AnalyzerPaneProvider() {
    this.tabSwitcher = TabSwitcherProxy.getInstance();
    this.analyzerNameLbl.setText(
        new AnalysisFactory().newInstance().currentAnalyzer().getClass().getName());

    ComponentOperatorRegistry.getInstance().register(AnalyzerTabOperator.class, this);
  }

  public JScrollPane get() {
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    panel.add(initAnalyzerNamePanel());
    panel.add(new JSeparator(JSeparator.HORIZONTAL));
    panel.add(initAnalysisChainPanel());

    tokenizerTF.setEditable(false);

    JScrollPane scrollPane = new JBScrollPane(panel);
    scrollPane.setOpaque(false);
    scrollPane.getViewport().setOpaque(false);
    return scrollPane;
  }

  private JPanel initAnalyzerNamePanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    panel.setOpaque(false);

    panel.add(new JLabel(MessageUtils.getLocalizedMessage("search_analyzer.label.name")));

    panel.add(analyzerNameLbl);

    JLabel changeLbl =
        new JLabel(MessageUtils.getLocalizedMessage("search_analyzer.hyperlink.change"));
    changeLbl.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            tabSwitcher.switchTab(TabbedPaneProvider.Tab.ANALYZER);
          }
        });
    panel.add(FontUtils.toLinkText(changeLbl));

    return panel;
  }

  private JPanel initAnalysisChainPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
    top.setOpaque(false);
    top.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    top.add(new JLabel(MessageUtils.getLocalizedMessage("search_analyzer.label.chain")));
    panel.add(top, BorderLayout.PAGE_START);

    JPanel center = new JPanel(new GridBagLayout());
    center.setOpaque(false);

    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.insets = JBUI.insets(5);

    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0.1;
    center.add(
        new JLabel(MessageUtils.getLocalizedMessage("search_analyzer.label.charfilters")), c);

    charFilterList.setVisibleRowCount(3);
    JScrollPane charFilterSP = new JBScrollPane(charFilterList);
    c.gridx = 1;
    c.gridy = 0;
    c.weightx = 0.5;
    center.add(charFilterSP, c);

    c.gridx = 0;
    c.gridy = 1;
    c.weightx = 0.1;
    center.add(new JLabel(MessageUtils.getLocalizedMessage("search_analyzer.label.tokenizer")), c);

    tokenizerTF.setColumns(30);
    tokenizerTF.setPreferredSize(new Dimension(400, 25));
    tokenizerTF.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
    c.gridx = 1;
    c.gridy = 1;
    c.weightx = 0.5;
    center.add(tokenizerTF, c);

    c.gridx = 0;
    c.gridy = 2;
    c.weightx = 0.1;
    center.add(
        new JLabel(MessageUtils.getLocalizedMessage("search_analyzer.label.tokenfilters")), c);

    tokenFilterList.setVisibleRowCount(3);
    JScrollPane tokenFilterSP = new JBScrollPane(tokenFilterList);
    c.gridx = 1;
    c.gridy = 2;
    c.weightx = 0.5;
    center.add(tokenFilterSP, c);

    panel.add(center, BorderLayout.CENTER);

    return panel;
  }

  @Override
  public void setAnalyzer(Analyzer analyzer) {
    analyzerNameLbl.setText(analyzer.getClass().getName());

    if (analyzer instanceof CustomAnalyzer) {
      CustomAnalyzer customAnalyzer = (CustomAnalyzer) analyzer;

      DefaultListModel<String> charFilterListModel = new DefaultListModel<>();
      customAnalyzer.getCharFilterFactories().stream()
          .map(f -> f.getClass().getSimpleName())
          .forEach(charFilterListModel::addElement);
      charFilterList.setModel(charFilterListModel);

      tokenizerTF.setText(customAnalyzer.getTokenizerFactory().getClass().getSimpleName());

      DefaultListModel<String> tokenFilterListModel = new DefaultListModel<>();
      customAnalyzer.getTokenFilterFactories().stream()
          .map(f -> f.getClass().getSimpleName())
          .forEach(tokenFilterListModel::addElement);
      tokenFilterList.setModel(tokenFilterListModel);

      charFilterList.setBackground(JBColor.WHITE);
      tokenizerTF.setBackground(JBColor.WHITE);
      tokenFilterList.setBackground(JBColor.WHITE);
    } else {
      charFilterList.setModel(new DefaultListModel<>());
      tokenizerTF.setText("");
      tokenFilterList.setModel(new DefaultListModel<>());

      charFilterList.setBackground(JBColor.LIGHT_GRAY);
      tokenizerTF.setBackground(JBColor.LIGHT_GRAY);
      tokenFilterList.setBackground(JBColor.LIGHT_GRAY);
    }
  }
}
