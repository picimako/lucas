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

package com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.analysis;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.picimako.lucas.LucasBundle;
import com.picimako.org.apache.lucene.luke.app.desktop.MessageBroker;
import com.picimako.org.apache.lucene.luke.app.desktop.components.AnalysisTabOperator;
import com.picimako.org.apache.lucene.luke.app.desktop.components.ComponentOperatorRegistry;
import com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.analysis.EditFiltersDialogFactory;
import com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.analysis.EditParamsDialogFactory;
import org.apache.lucene.luke.app.desktop.components.dialog.analysis.EditFiltersMode;
import org.apache.lucene.luke.app.desktop.components.dialog.analysis.EditParamsMode;
import org.apache.lucene.luke.app.desktop.util.FontUtils;
import org.apache.lucene.luke.app.desktop.util.ListUtils;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.lang.Callable;
import org.apache.lucene.luke.models.analysis.Analysis;
import org.apache.lucene.luke.models.analysis.CustomAnalyzerConfig;
import org.apache.lucene.util.SuppressForbidden;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Provider of the custom analyzer panel
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.fragments.analysis.CustomAnalyzerPanelProvider}.
 */
public final class CustomAnalyzerPanelProvider implements CustomAnalyzerPanelOperator {

  private final ComponentOperatorRegistry operatorRegistry;

  private final MessageBroker messageBroker;

  private final JTextField confDirTF = new JTextField();

  private final JFileChooser fileChooser = new JFileChooser();

  private final JButton confDirBtn = new JButton(AllIcons.Actions.MenuOpen);

  private final JButton buildBtn = new JButton(AllIcons.Toolwindows.ToolWindowBuild);

  private final JLabel loadJarLbl = new JLabel();

  private final JList<String> selectedCfList = new JBList<>();

  private final JButton cfEditBtn = new JButton(AllIcons.Actions.Edit);

  private final JComboBox<String> cfFactoryCombo = new ComboBox<>();

  private final JTextField selectedTokTF = new JTextField();

  private final JButton tokEditBtn = new JButton(AllIcons.Actions.Edit);

  private final JComboBox<String> tokFactoryCombo = new ComboBox<>();

  private final JList<String> selectedTfList = new JBList<>();

  private final JButton tfEditBtn = new JButton(AllIcons.Actions.Edit);

  private final JComboBox<String> tfFactoryCombo = new ComboBox<>();

  private final ListenerFunctions listeners = new ListenerFunctions();

  private final List<Map<String, String>> cfParamsList = new ArrayList<>();

  private final Map<String, String> tokParams = new HashMap<>();

  private final List<Map<String, String>> tfParamsList = new ArrayList<>();

  private final Project project;

  private JPanel containerPanel;

  private Analysis analysisModel;

  public CustomAnalyzerPanelProvider(Project project) {
    this.project = project;
    this.operatorRegistry = ComponentOperatorRegistry.getInstance();
    this.messageBroker = MessageBroker.getInstance();

    operatorRegistry.register(CustomAnalyzerPanelOperator.class, this);

    cfFactoryCombo.addActionListener(listeners::addCharFilter);
    tokFactoryCombo.addActionListener(listeners::setTokenizer);
    tfFactoryCombo.addActionListener(listeners::addTokenFilter);
  }

  public JPanel get() {
    if (containerPanel == null) {
      containerPanel = new JPanel();
      containerPanel.setOpaque(false);
      containerPanel.setLayout(new BorderLayout());
      containerPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

      containerPanel.add(initCustomAnalyzerHeaderPanel(), BorderLayout.PAGE_START);
      containerPanel.add(initCustomAnalyzerChainPanel(), BorderLayout.CENTER);
    }

    return containerPanel;
  }

  private JPanel initCustomAnalyzerHeaderPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    panel.setOpaque(false);

    panel.add(new JLabel(MessageUtils.getLocalizedMessage("analysis.label.config_dir")));
    confDirTF.setColumns(30);
    confDirTF.setPreferredSize(new Dimension(200, 30));
    panel.add(confDirTF);
    confDirBtn.setText(MessageUtils.getLocalizedMessage("analysis.button.browse"));
    confDirBtn.setMargin(JBUI.insets(3));
    confDirBtn.addActionListener(listeners::chooseConfigDir);
    panel.add(confDirBtn);
    buildBtn.setText(LucasBundle.message("analysis.button.build_analyzer"));
    buildBtn.setMargin(JBUI.insets(3));
    buildBtn.addActionListener(listeners::buildAnalyzer);
    panel.add(buildBtn);
    loadJarLbl.setText(MessageUtils.getLocalizedMessage("analysis.hyperlink.load_jars"));
    loadJarLbl.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            listeners.loadExternalJars(e);
          }
        });
    panel.add(FontUtils.toLinkText(loadJarLbl));

    return panel;
  }

  private JPanel initCustomAnalyzerChainPanel() {
    JPanel panel = new JPanel(new GridLayout(1, 1));
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    panel.add(initCustomChainConfigPanel());

    return panel;
  }

  private JPanel initCustomChainConfigPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createLineBorder(JBColor.BLACK));

    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;

    GridBagConstraints sepc = new GridBagConstraints();
    sepc.fill = GridBagConstraints.HORIZONTAL;
    sepc.weightx = 1.0;
    sepc.gridwidth = GridBagConstraints.REMAINDER;

    // char filters
    JLabel cfLbl =
        new JLabel(MessageUtils.getLocalizedMessage("analysis_custom.label.charfilters"));
    cfLbl.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 3));
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.CENTER;
    panel.add(cfLbl, c);

    c.gridx = 1;
    c.gridy = 0;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(new JLabel(MessageUtils.getLocalizedMessage("analysis_custom.label.selected")), c);

    selectedCfList.setVisibleRowCount(1);
    JScrollPane selectedPanel = new JBScrollPane(selectedCfList);
    c.gridx = 2;
    c.gridy = 0;
    c.gridwidth = 5;
    c.gridheight = 1;
    c.weightx = 0.5;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(selectedPanel, c);

    cfEditBtn.setText(MessageUtils.getLocalizedMessage("analysis_custom.label.edit"));
    cfEditBtn.setMargin(JBUI.insets(2, 4));
    cfEditBtn.setEnabled(false);
    cfEditBtn.addActionListener(listeners::editCharFilters);
    c.fill = GridBagConstraints.NONE;
    c.gridx = 7;
    c.gridy = 0;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.CENTER;
    panel.add(cfEditBtn, c);

    JLabel cfAddLabel = new JLabel(MessageUtils.getLocalizedMessage("analysis_custom.label.add"));
    cfAddLabel.setIcon(AllIcons.General.Add);
    cfAddLabel.setHorizontalAlignment(JLabel.LEFT);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 1;
    c.gridy = 2;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(cfAddLabel, c);

    c.gridx = 2;
    c.gridy = 2;
    c.gridwidth = 5;
    c.gridheight = 1;
    c.weightx = 0.5;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(cfFactoryCombo, c);

    // separator
    sepc.gridx = 0;
    sepc.gridy = 3;
    sepc.anchor = GridBagConstraints.LINE_START;
    panel.add(new JSeparator(JSeparator.HORIZONTAL), sepc);

    // tokenizer
    JLabel tokLabel =
        new JLabel(MessageUtils.getLocalizedMessage("analysis_custom.label.tokenizer"));
    tokLabel.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 3));
    c.gridx = 0;
    c.gridy = 4;
    c.gridwidth = 1;
    c.gridheight = 2;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.CENTER;
    panel.add(tokLabel, c);

    c.gridx = 1;
    c.gridy = 4;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(new JLabel(MessageUtils.getLocalizedMessage("analysis_custom.label.selected")), c);

    selectedTokTF.setColumns(15);
    selectedTokTF.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));
    selectedTokTF.setText("standard");
    selectedTokTF.setEditable(false);
    c.gridx = 2;
    c.gridy = 4;
    c.gridwidth = 5;
    c.gridheight = 1;
    c.weightx = 0.5;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(selectedTokTF, c);

    tokEditBtn.setText(MessageUtils.getLocalizedMessage("analysis_custom.label.edit"));
    tokEditBtn.setMargin(JBUI.insets(2, 4));
    tokEditBtn.addActionListener(listeners::editTokenizer);
    c.fill = GridBagConstraints.NONE;
    c.gridx = 7;
    c.gridy = 4;
    c.gridwidth = 2;
    c.gridheight = 1;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.CENTER;
    panel.add(tokEditBtn, c);

    JLabel setTokLabel = new JLabel(MessageUtils.getLocalizedMessage("analysis_custom.label.set"));
    setTokLabel.setIcon(AllIcons.Actions.PinTab);
    setTokLabel.setHorizontalAlignment(SwingConstants.LEFT);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 1;
    c.gridy = 6;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(setTokLabel, c);

    c.gridx = 2;
    c.gridy = 6;
    c.gridwidth = 5;
    c.gridheight = 1;
    c.weightx = 0.5;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(tokFactoryCombo, c);

    // separator
    sepc.gridx = 0;
    sepc.gridy = 7;
    sepc.anchor = GridBagConstraints.LINE_START;
    panel.add(new JSeparator(JSeparator.HORIZONTAL), sepc);

    // token filters
    JLabel tfLbl =
        new JLabel(MessageUtils.getLocalizedMessage("analysis_custom.label.tokenfilters"));
    tfLbl.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 3));
    c.gridx = 0;
    c.gridy = 8;
    c.gridwidth = 1;
    c.gridheight = 2;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.CENTER;
    panel.add(tfLbl, c);

    c.gridx = 1;
    c.gridy = 8;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(new JLabel(MessageUtils.getLocalizedMessage("analysis_custom.label.selected")), c);

    selectedTfList.setVisibleRowCount(1);
    JScrollPane selectedTfPanel = new JBScrollPane(selectedTfList);
    c.gridx = 2;
    c.gridy = 8;
    c.gridwidth = 5;
    c.gridheight = 1;
    c.weightx = 0.5;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(selectedTfPanel, c);

    tfEditBtn.setText(MessageUtils.getLocalizedMessage("analysis_custom.label.edit"));
    tfEditBtn.setMargin(JBUI.insets(2, 4));
    tfEditBtn.setEnabled(false);
    tfEditBtn.addActionListener(listeners::editTokenFilters);
    c.fill = GridBagConstraints.NONE;
    c.gridx = 7;
    c.gridy = 8;
    c.gridwidth = 2;
    c.gridheight = 1;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.CENTER;
    panel.add(tfEditBtn, c);

    JLabel tfAddLabel = new JLabel(MessageUtils.getLocalizedMessage("analysis_custom.label.add"));
    tfAddLabel.setIcon(AllIcons.General.Add);
    tfAddLabel.setHorizontalAlignment(JLabel.LEFT);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 1;
    c.gridy = 10;
    c.gridwidth = 1;
    c.gridheight = 1;
    c.weightx = 0.1;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(tfAddLabel, c);

    c.gridx = 2;
    c.gridy = 10;
    c.gridwidth = 5;
    c.gridheight = 1;
    c.weightx = 0.5;
    c.weighty = 0.5;
    c.anchor = GridBagConstraints.LINE_END;
    panel.add(tfFactoryCombo, c);

    return panel;
  }

  // control methods

  @SuppressForbidden(reason = "JFilechooser#getSelectedFile() returns java.io.File")
  private void chooseConfigDir() {
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    int ret = fileChooser.showOpenDialog(containerPanel);
    if (ret == JFileChooser.APPROVE_OPTION) {
      File dir = fileChooser.getSelectedFile();
      confDirTF.setText(dir.getAbsolutePath());
    }
  }

  @SuppressForbidden(reason = "JFilechooser#getSelectedFiles() returns java.io.File[]")
  private void loadExternalJars() {
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(true);

    int ret = fileChooser.showOpenDialog(containerPanel);
    if (ret == JFileChooser.APPROVE_OPTION) {
      File[] files = fileChooser.getSelectedFiles();
      analysisModel.addExternalJars(Arrays.stream(files).map(File::getAbsolutePath).toList());
      operatorRegistry
          .get(CustomAnalyzerPanelOperator.class)
          .ifPresent(CustomAnalyzerPanelOperator::resetAnalysisComponents);
      messageBroker.showStatusMessage("External jars were added.");
    }
  }

  private void buildAnalyzer() {
    List<String> charFilters = ListUtils.getAllItems(selectedCfList);
    assert charFilters.size() == cfParamsList.size();

    List<String> tokenFilters = ListUtils.getAllItems(selectedTfList);
    assert tokenFilters.size() == tfParamsList.size();

    String tokenizerName = selectedTokTF.getText();
    CustomAnalyzerConfig.Builder builder =
        new CustomAnalyzerConfig.Builder(tokenizerName, tokParams).configDir(confDirTF.getText());
    IntStream.range(0, charFilters.size())
        .forEach(i -> builder.addCharFilterConfig(charFilters.get(i), cfParamsList.get(i)));
    IntStream.range(0, tokenFilters.size())
        .forEach(i -> builder.addTokenFilterConfig(tokenFilters.get(i), tfParamsList.get(i)));
    CustomAnalyzerConfig config = builder.build();

    operatorRegistry
        .get(AnalysisTabOperator.class)
        .ifPresent(
            operator -> {
              operator.setAnalyzerByCustomConfiguration(config);
              messageBroker.showStatusMessage(
                  MessageUtils.getLocalizedMessage("analysis.message.build_success"));
              buildBtn.setEnabled(false);
            });
  }

  private void addCharFilter() {
    if (Objects.isNull(cfFactoryCombo.getSelectedItem())
        || cfFactoryCombo.getSelectedItem() == "") {
      return;
    }

    int targetIndex = selectedCfList.getModel().getSize();
    String selectedItem = (String) cfFactoryCombo.getSelectedItem();
    List<String> updatedList = ListUtils.getAllItems(selectedCfList);
    updatedList.add(selectedItem);
    cfParamsList.add(new HashMap<>());

    //Disabled here because by default the Luke standalone application seems to run with -disableassertions,
    // and this assertion fails there as well when assertions are enabled.
    //assert selectedCfList.getModel().getSize() == cfParamsList.size();

    showEditParamsDialog(
        MessageUtils.getLocalizedMessage("analysis.dialog.title.char_filter_params"),
        EditParamsMode.CHARFILTER,
        targetIndex,
        selectedItem,
        cfParamsList.get(cfParamsList.size() - 1),
        () -> {
          selectedCfList.setModel(new DefaultComboBoxModel<>(updatedList.toArray(new String[0])));
          cfFactoryCombo.setSelectedItem("");
          cfEditBtn.setEnabled(true);
          buildBtn.setEnabled(true);
        });
  }

  private void setTokenizer() {
    if (Objects.isNull(tokFactoryCombo.getSelectedItem())
        || tokFactoryCombo.getSelectedItem() == "") {
      return;
    }

    String selectedItem = (String) tokFactoryCombo.getSelectedItem();
    showEditParamsDialog(
        MessageUtils.getLocalizedMessage("analysis.dialog.title.tokenizer_params"),
        EditParamsMode.TOKENIZER,
        -1,
        selectedItem,
        Collections.emptyMap(),
        () -> {
          selectedTokTF.setText(selectedItem);
          tokFactoryCombo.setSelectedItem("");
          buildBtn.setEnabled(true);
        });
  }

  private void addTokenFilter() {
    if (Objects.isNull(tfFactoryCombo.getSelectedItem())
        || tfFactoryCombo.getSelectedItem() == "") {
      return;
    }

    int targetIndex = selectedTfList.getModel().getSize();
    String selectedItem = (String) tfFactoryCombo.getSelectedItem();
    List<String> updatedList = ListUtils.getAllItems(selectedTfList);
    updatedList.add(selectedItem);
    tfParamsList.add(new HashMap<>());

    //Disabled here because by default the Luke standalone application seems to run with -disableassertions,
    // and this assertion fails there as well when assertions are enabled.
    //assert selectedTfList.getModel().getSize() == tfParamsList.size();

    showEditParamsDialog(
        MessageUtils.getLocalizedMessage("analysis.dialog.title.token_filter_params"),
        EditParamsMode.TOKENFILTER,
        targetIndex,
        selectedItem,
        tfParamsList.get(tfParamsList.size() - 1),
        () -> {
          selectedTfList.setModel(
              new DefaultComboBoxModel<>(updatedList.toArray(new String[updatedList.size()])));
          tfFactoryCombo.setSelectedItem("");
          tfEditBtn.setEnabled(true);
          buildBtn.setEnabled(true);
        });
  }

  private void showEditParamsDialog(
      String title,
      EditParamsMode mode,
      int targetIndex,
      String selectedItem,
      Map<String, String> params,
      Callable callback) {
    EditParamsDialogFactory factory = new EditParamsDialogFactory(project, title, selectedItem, params);
    factory.setMode(mode);
    factory.setTargetIndex(targetIndex);
    factory.setCallback(callback);
    factory.show();
  }

  private void editCharFilters() {
    List<String> filters = ListUtils.getAllItems(selectedCfList);
    showEditFiltersDialog(
        EditFiltersMode.CHARFILTER,
        filters,
        () -> {
          cfEditBtn.setEnabled(selectedCfList.getModel().getSize() > 0);
          buildBtn.setEnabled(true);
        });
  }

  private void editTokenizer() {
    String selectedItem = selectedTokTF.getText();
    showEditParamsDialog(
        MessageUtils.getLocalizedMessage("analysis.dialog.title.tokenizer_params"),
        EditParamsMode.TOKENIZER,
        -1,
        selectedItem,
        tokParams,
        () -> buildBtn.setEnabled(true));
  }

  private void editTokenFilters() {
    List<String> filters = ListUtils.getAllItems(selectedTfList);
    showEditFiltersDialog(
        EditFiltersMode.TOKENFILTER,
        filters,
        () -> {
          tfEditBtn.setEnabled(selectedTfList.getModel().getSize() > 0);
          buildBtn.setEnabled(true);
        });
  }

  private void showEditFiltersDialog(
      EditFiltersMode mode, List<String> selectedFilters, Callable callback) {
    String title =
        (mode == EditFiltersMode.CHARFILTER)
            ? MessageUtils.getLocalizedMessage("analysis.dialog.title.selected_char_filter")
            : MessageUtils.getLocalizedMessage("analysis.dialog.title.selected_token_filter");
    new EditFiltersDialogFactory(project, title, selectedFilters, callback, mode).show();
  }

  @Override
  public void setAnalysisModel(Analysis model) {
    analysisModel = model;
  }

  @Override
  public void resetAnalysisComponents() {
    setAvailableCharFilterFactories();
    setAvailableTokenizerFactories();
    setAvailableTokenFilterFactories();
    buildBtn.setEnabled(true);
  }

  private void setAvailableCharFilterFactories() {
    Collection<String> charFilters = analysisModel.getAvailableCharFilters();
    String[] charFilterNames = new String[charFilters.size() + 1];
    charFilterNames[0] = "";
    System.arraycopy(charFilters.toArray(new String[0]), 0, charFilterNames, 1, charFilters.size());
    cfFactoryCombo.setModel(new DefaultComboBoxModel<>(charFilterNames));
  }

  private void setAvailableTokenizerFactories() {
    Collection<String> tokenizers = analysisModel.getAvailableTokenizers();
    String[] tokenizerNames = new String[tokenizers.size() + 1];
    tokenizerNames[0] = "";
    System.arraycopy(tokenizers.toArray(new String[0]), 0, tokenizerNames, 1, tokenizers.size());
    tokFactoryCombo.setModel(new DefaultComboBoxModel<>(tokenizerNames));
  }

  private void setAvailableTokenFilterFactories() {
    Collection<String> tokenFilters = analysisModel.getAvailableTokenFilters();
    String[] tokenFilterNames = new String[tokenFilters.size() + 1];
    tokenFilterNames[0] = "";
    System.arraycopy(
        tokenFilters.toArray(new String[0]), 0, tokenFilterNames, 1, tokenFilters.size());
    tfFactoryCombo.setModel(new DefaultComboBoxModel<>(tokenFilterNames));
  }

  @Override
  public void updateCharFilters(List<Integer> deletedIndexes) {
    // update filters
    List<String> filters = ListUtils.getAllItems(selectedCfList);
    String[] updatedFilters =
        IntStream.range(0, filters.size())
            .filter(i -> !deletedIndexes.contains(i))
            .mapToObj(filters::get)
            .toArray(String[]::new);
    selectedCfList.setModel(new DefaultComboBoxModel<>(updatedFilters));
    // update parameters map for each filter
    List<Map<String, String>> updatedParamList =
        IntStream.range(0, cfParamsList.size())
            .filter(i -> !deletedIndexes.contains(i))
            .mapToObj(cfParamsList::get)
            .toList();
    cfParamsList.clear();
    cfParamsList.addAll(updatedParamList);
    assert selectedCfList.getModel().getSize() == cfParamsList.size();
  }

  @Override
  public void updateTokenFilters(List<Integer> deletedIndexes) {
    // update filters
    List<String> filters = ListUtils.getAllItems(selectedTfList);
    String[] updatedFilters =
        IntStream.range(0, filters.size())
            .filter(i -> !deletedIndexes.contains(i))
            .mapToObj(filters::get)
            .toArray(String[]::new);
    selectedTfList.setModel(new DefaultComboBoxModel<>(updatedFilters));
    // update parameters map for each filter
    List<Map<String, String>> updatedParamList =
        IntStream.range(0, tfParamsList.size())
            .filter(i -> !deletedIndexes.contains(i))
            .mapToObj(tfParamsList::get)
            .toList();
    tfParamsList.clear();
    tfParamsList.addAll(updatedParamList);
    assert selectedTfList.getModel().getSize() == tfParamsList.size();
  }

  @Override
  public Map<String, String> getCharFilterParams(int index) {
    if (index < 0 || index > cfParamsList.size()) {
      throw new IllegalArgumentException();
    }
    return Map.copyOf(cfParamsList.get(index));
  }

  @Override
  public void updateCharFilterParams(int index, Map<String, String> updatedParams) {
    if (index < 0 || index > cfParamsList.size()) {
      throw new IllegalArgumentException();
    }
    if (index == cfParamsList.size()) {
      cfParamsList.add(new HashMap<>());
    }
    cfParamsList.get(index).clear();
    cfParamsList.get(index).putAll(updatedParams);
  }

  @Override
  public void updateTokenizerParams(Map<String, String> updatedParams) {
    tokParams.clear();
    tokParams.putAll(updatedParams);
  }

  @Override
  public Map<String, String> getTokenFilterParams(int index) {
    if (index < 0 || index > tfParamsList.size()) {
      throw new IllegalArgumentException();
    }
    return Map.copyOf(tfParamsList.get(index));
  }

  @Override
  public void updateTokenFilterParams(int index, Map<String, String> updatedParams) {
    if (index < 0 || index > tfParamsList.size()) {
      throw new IllegalArgumentException();
    }
    if (index == tfParamsList.size()) {
      tfParamsList.add(new HashMap<>());
    }
    tfParamsList.get(index).clear();
    tfParamsList.get(index).putAll(updatedParams);
  }

  private class ListenerFunctions {

    void chooseConfigDir(ActionEvent e) {
      CustomAnalyzerPanelProvider.this.chooseConfigDir();
    }

    void loadExternalJars(MouseEvent e) {
      CustomAnalyzerPanelProvider.this.loadExternalJars();
    }

    void buildAnalyzer(ActionEvent e) {
      CustomAnalyzerPanelProvider.this.buildAnalyzer();
    }

    void addCharFilter(ActionEvent e) {
      CustomAnalyzerPanelProvider.this.addCharFilter();
    }

    void setTokenizer(ActionEvent e) {
      CustomAnalyzerPanelProvider.this.setTokenizer();
    }

    void addTokenFilter(ActionEvent e) {
      CustomAnalyzerPanelProvider.this.addTokenFilter();
    }

    void editCharFilters(ActionEvent e) {
      CustomAnalyzerPanelProvider.this.editCharFilters();
    }

    void editTokenizer(ActionEvent e) {
      CustomAnalyzerPanelProvider.this.editTokenizer();
    }

    void editTokenFilters(ActionEvent e) {
      CustomAnalyzerPanelProvider.this.editTokenFilters();
    }
  }
}
