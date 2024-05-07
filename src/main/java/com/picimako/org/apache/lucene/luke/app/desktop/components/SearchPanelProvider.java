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
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.MessageBroker;
import com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.search.ExplainDialogFactory;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.AnalyzerPaneProvider;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.FieldValuesPaneProvider;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.FieldValuesTabOperator;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.MLTPaneProvider;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.MLTTabOperator;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.QueryParserPaneProvider;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.QueryParserTabOperator;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.SimilarityPaneProvider;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.SimilarityTabOperator;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.SortPaneProvider;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.search.SortTabOperator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.luke.app.IndexObserver;
import org.apache.lucene.luke.app.LukeState;
import org.apache.lucene.luke.app.desktop.components.TableColumnInfo;
import org.apache.lucene.luke.app.desktop.components.TableModelBase;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.StringUtils;
import org.apache.lucene.luke.app.desktop.util.TableUtils;
import org.apache.lucene.luke.models.LukeException;
import org.apache.lucene.luke.models.search.MLTConfig;
import org.apache.lucene.luke.models.search.QueryParserConfig;
import org.apache.lucene.luke.models.search.Search;
import org.apache.lucene.luke.models.search.SearchFactory;
import org.apache.lucene.luke.models.search.SearchResults;
import org.apache.lucene.luke.models.search.SimilarityConfig;
import org.apache.lucene.luke.models.tools.IndexTools;
import org.apache.lucene.luke.models.tools.IndexToolsFactory;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TotalHits;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Provider of the Search panel
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.SearchPanelProvider}.
 */
public final class SearchPanelProvider implements SearchTabOperator {

  private static final int DEFAULT_PAGE_SIZE = 10;

  private final SearchFactory searchFactory;

  private final IndexToolsFactory toolsFactory;

  private final IndexHandler indexHandler;

  private final MessageBroker messageBroker;

  private final TabSwitcherProxy tabSwitcher;

  private final ComponentOperatorRegistry operatorRegistry;

  private final JTabbedPane tabbedPane = new JBTabbedPane();

  private final JScrollPane qparser;

  private final JScrollPane analyzer;

  private final JScrollPane similarity;

  private final JScrollPane sort;

  private final JScrollPane values;

  private final JScrollPane mlt;

  private final JCheckBox termQueryCB = new JCheckBox();

  private final JTextArea queryStringTA = new JTextArea();

  private final JTextArea parsedQueryTA = new JTextArea();

  private final JButton parseBtn = new JButton(AllIcons.Actions.ShowAsTree);

  private final JCheckBox rewriteCB = new JCheckBox();

  private final JButton searchBtn = new JButton(AllIcons.Actions.Find);

  private final JCheckBox exactHitsCntCB = new JCheckBox();

  private final JButton mltBtn = new JButton(AllIcons.Nodes.Related);

  private final JFormattedTextField mltDocFTF = new JFormattedTextField();

  private final JLabel totalHitsLbl = new JLabel();

  private final JLabel startLbl = new JLabel();

  private final JLabel endLbl = new JLabel();

  private final JButton prevBtn = new JButton();

  private final JButton nextBtn = new JButton();

  private final JButton delBtn = new JButton(AllIcons.Actions.GC);

  private final JTable resultsTable = new JBTable();

  private final ListenerFunctions listeners = new ListenerFunctions();

  private Search searchModel;

  private IndexTools toolsModel;

  private final Project project;

  public SearchPanelProvider(Project project) {
    this.project = project;
    this.searchFactory = new SearchFactory();
    this.toolsFactory = new IndexToolsFactory();
    this.indexHandler = IndexHandler.getInstance();
    this.messageBroker = MessageBroker.getInstance();
    this.tabSwitcher = TabSwitcherProxy.getInstance();
    this.operatorRegistry = ComponentOperatorRegistry.getInstance();
    this.qparser = new QueryParserPaneProvider().get();
    this.analyzer = new AnalyzerPaneProvider().get();
    this.similarity = new SimilarityPaneProvider().get();
    this.sort = new SortPaneProvider().get();
    this.values = new FieldValuesPaneProvider().get();
    this.mlt = new MLTPaneProvider().get();

    indexHandler.addObserver(new Observer());
    operatorRegistry.register(SearchTabOperator.class, this);
  }

  public JPanel get() {
    JPanel panel = new JPanel(new GridLayout(1, 1));
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createLineBorder(JBColor.GRAY));

    JSplitPane splitPane =
        new JSplitPane(JSplitPane.VERTICAL_SPLIT, initUpperPanel(), initLowerPanel());
    splitPane.setOpaque(false);
    splitPane.setDividerLocation(350);
    panel.add(splitPane);

    return panel;
  }

  private JSplitPane initUpperPanel() {
    JSplitPane splitPane =
        new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, initQuerySettingsPane(), initQueryPane());
    splitPane.setOpaque(false);
    splitPane.setDividerLocation(570);
    return splitPane;
  }

  private JPanel initQuerySettingsPane() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    JLabel label = new JLabel(MessageUtils.getLocalizedMessage("search.label.settings"));
    panel.add(label, BorderLayout.PAGE_START);

    tabbedPane.addTab("Query Parser", qparser);
    tabbedPane.addTab("Analyzer", analyzer);
    tabbedPane.addTab("Similarity", similarity);
    tabbedPane.addTab("Sort", sort);
    tabbedPane.addTab("Field Values", values);
    tabbedPane.addTab("More Like This", mlt);

    panel.add(tabbedPane, BorderLayout.CENTER);

    return panel;
  }

  private JPanel initQueryPane() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.LINE_START;

    JLabel labelQE = new JLabel(MessageUtils.getLocalizedMessage("search.label.expression"));
    c.gridx = 0;
    c.gridy = 0;
    c.gridwidth = 2;
    c.weightx = 0.5;
    c.insets = JBUI.insets(2, 0, 2, 2);
    panel.add(labelQE, c);

    termQueryCB.setText(MessageUtils.getLocalizedMessage("search.checkbox.term"));
    termQueryCB.addActionListener(listeners::toggleTermQuery);
    termQueryCB.setOpaque(false);
    c.gridx = 2;
    c.gridy = 0;
    c.gridwidth = 1;
    c.weightx = 0.2;
    c.insets = JBUI.insets(2, 0, 2, 2);
    panel.add(termQueryCB, c);

    queryStringTA.setRows(3);
    queryStringTA.setLineWrap(true);
    queryStringTA.setText("*:*");
    c.gridx = 0;
    c.gridy = 1;
    c.gridwidth = 3;
    c.weightx = 0.0;
    c.insets = JBUI.insets(2, 0, 2, 2);
    panel.add(
        new JScrollPane(
            queryStringTA,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
        c);

    JLabel labelPQ = new JLabel(MessageUtils.getLocalizedMessage("search.label.parsed"));
    c.gridx = 0;
    c.gridy = 2;
    c.gridwidth = 3;
    c.weightx = 0.0;
    c.insets = JBUI.insets(8, 0, 2, 2);
    panel.add(labelPQ, c);

    parsedQueryTA.setRows(3);
    parsedQueryTA.setLineWrap(true);
    parsedQueryTA.setEditable(false);
    c.gridx = 0;
    c.gridy = 3;
    c.gridwidth = 3;
    c.weightx = 0.0;
    c.insets = JBUI.insets(2, 0, 2, 2);
    panel.add(new JScrollPane(parsedQueryTA), c);

    parseBtn.setText(MessageUtils.getLocalizedMessage("search.button.parse"));
    parseBtn.setMargin(JBUI.insets(3, 0));
    parseBtn.addActionListener(listeners::execParse);
    c.gridx = 0;
    c.gridy = 4;
    c.gridwidth = 1;
    c.weightx = 0.2;
    c.insets = JBUI.insets(5, 0, 0, 2);
    panel.add(parseBtn, c);

    rewriteCB.setText(MessageUtils.getLocalizedMessage("search.checkbox.rewrite"));
    rewriteCB.setOpaque(false);
    c.gridx = 1;
    c.gridy = 4;
    c.gridwidth = 2;
    c.weightx = 0.2;
    c.insets = JBUI.insets(5, 0, 0, 2);
    panel.add(rewriteCB, c);

    searchBtn.setText(MessageUtils.getLocalizedMessage("search.button.search"));
    searchBtn.setMargin(JBUI.insets(3, 0));
    searchBtn.addActionListener(listeners::execSearch);
    c.gridx = 0;
    c.gridy = 5;
    c.gridwidth = 1;
    c.weightx = 0.2;
    c.insets = JBUI.insets(5, 0);
    panel.add(searchBtn, c);

    exactHitsCntCB.setText(MessageUtils.getLocalizedMessage("search.checkbox.exact_hits_cnt"));
    exactHitsCntCB.setOpaque(false);
    c.gridx = 1;
    c.gridy = 5;
    c.gridwidth = 2;
    c.weightx = 0.2;
    c.insets = JBUI.insets(5, 0, 0, 2);
    panel.add(exactHitsCntCB, c);

    mltBtn.setText(MessageUtils.getLocalizedMessage("search.button.mlt"));
    mltBtn.setMargin(JBUI.insets(3, 0));
    mltBtn.addActionListener(listeners::execMLTSearch);
    c.gridx = 0;
    c.gridy = 6;
    c.gridwidth = 1;
    c.weightx = 0.3;
    c.insets = JBUI.insets(10, 0, 2, 0);
    panel.add(mltBtn, c);

    JPanel docNo = new JPanel(new FlowLayout(FlowLayout.LEADING));
    docNo.setOpaque(false);
    JLabel docNoLabel = new JLabel("with doc #");
    docNo.add(docNoLabel);
    mltDocFTF.setColumns(8);
    mltDocFTF.setValue(0);
    docNo.add(mltDocFTF);
    c.gridx = 1;
    c.gridy = 6;
    c.gridwidth = 2;
    c.weightx = 0.3;
    c.insets = JBUI.insets(8, 0, 0, 2);
    panel.add(docNo, c);

    return panel;
  }

  private JPanel initLowerPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    panel.add(initSearchResultsHeaderPane(), BorderLayout.PAGE_START);
    panel.add(initSearchResultsTablePane(), BorderLayout.CENTER);

    return panel;
  }

  private JPanel initSearchResultsHeaderPane() {
    JPanel panel = new JPanel(new GridLayout(1, 2));
    panel.setOpaque(false);

    JLabel label = new JLabel(MessageUtils.getLocalizedMessage("search.label.results"));
    label.setIcon(AllIcons.Nodes.DataTables);
    label.setHorizontalTextPosition(SwingConstants.RIGHT);
    label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
    panel.add(label);

    JPanel resultsInfo = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    resultsInfo.setOpaque(false);
    resultsInfo.setOpaque(false);

    JLabel totalLabel = new JLabel(MessageUtils.getLocalizedMessage("search.label.total"));
    resultsInfo.add(totalLabel);

    totalHitsLbl.setText("?");
    resultsInfo.add(totalHitsLbl);

    prevBtn.setIcon(AllIcons.General.ArrowLeft);
    prevBtn.setMargin(JBUI.insets(5, 0));
    prevBtn.setPreferredSize(new Dimension(30, 20));
    prevBtn.setEnabled(false);
    prevBtn.addActionListener(listeners::prevPage);
    resultsInfo.add(prevBtn);

    startLbl.setText("0");
    resultsInfo.add(startLbl);

    resultsInfo.add(new JLabel(" ~ "));

    endLbl.setText("0");
    resultsInfo.add(endLbl);

    nextBtn.setIcon(AllIcons.General.ArrowRight);
    nextBtn.setMargin(JBUI.insets(3, 0));
    nextBtn.setPreferredSize(new Dimension(30, 20));
    nextBtn.setEnabled(false);
    nextBtn.addActionListener(listeners::nextPage);
    resultsInfo.add(nextBtn);

    JSeparator sep = new JSeparator(JSeparator.VERTICAL);
    sep.setPreferredSize(new Dimension(5, 1));
    resultsInfo.add(sep);

    delBtn.setText(MessageUtils.getLocalizedMessage("search.button.del_all"));
    delBtn.setMargin(JBUI.insets(5, 0));
    delBtn.setEnabled(false);
    delBtn.addActionListener(listeners::confirmDeletion);
    resultsInfo.add(delBtn);

    panel.add(resultsInfo, BorderLayout.CENTER);

    return panel;
  }

  private JPanel initSearchResultsTablePane() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JPanel note = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 2));
    note.setOpaque(false);
    note.add(new JLabel(MessageUtils.getLocalizedMessage("search.label.results.note")));
    panel.add(note, BorderLayout.PAGE_START);

    TableUtils.setupTable(
        resultsTable,
        ListSelectionModel.SINGLE_SELECTION,
        new SearchResultsTableModel(),
        new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            listeners.showContextMenuInResultsTable(e);
          }
        },
        SearchResultsTableModel.Column.DOCID.getColumnWidth(),
        SearchResultsTableModel.Column.SCORE.getColumnWidth());
    JScrollPane scrollPane = new JBScrollPane(resultsTable);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  // control methods

  private void toggleTermQuery() {
    if (termQueryCB.isSelected()) {
      enableTermQuery();
    } else {
      disableTermQuery();
    }
  }

  private void enableTermQuery() {
    tabbedPane.setEnabledAt(Tab.QPARSER.index(), false);
    tabbedPane.setEnabledAt(Tab.ANALYZER.index(), false);
    tabbedPane.setEnabledAt(Tab.SIMILARITY.index(), false);
    if (tabbedPane.getSelectedIndex() == Tab.QPARSER.index()
        || tabbedPane.getSelectedIndex() == Tab.ANALYZER.index()
        || tabbedPane.getSelectedIndex() == Tab.SIMILARITY.index()
        || tabbedPane.getSelectedIndex() == Tab.MLT.index()) {
      tabbedPane.setSelectedIndex(Tab.SORT.index());
    }
    parseBtn.setEnabled(false);
    rewriteCB.setEnabled(false);
  }

  private void disableTermQuery() {
    tabbedPane.setEnabledAt(Tab.QPARSER.index(), true);
    tabbedPane.setEnabledAt(Tab.ANALYZER.index(), true);
    tabbedPane.setEnabledAt(Tab.SIMILARITY.index(), true);
    parseBtn.setEnabled(true);
    rewriteCB.setEnabled(true);
  }

  private void execParse() {
    Query query = parse(rewriteCB.isSelected());
    parsedQueryTA.setText(query.toString());
    messageBroker.clearStatusMessage();
  }

  private void doSearch() {
    Query query;
    if (termQueryCB.isSelected()) {
      // term query
      if (StringUtils.isNullOrEmpty(queryStringTA.getText())) {
        throw new LukeException("Query is not set.");
      }
      String[] tmp = queryStringTA.getText().split(":");
      if (tmp.length < 2) {
        throw new LukeException(
            String.format(Locale.ENGLISH, "Invalid query [ %s ]", queryStringTA.getText()));
      }
      query = new TermQuery(new Term(tmp[0].trim(), tmp[1].trim()));
    } else {
      query = parse(false);
    }
    SimilarityConfig simConfig =
        operatorRegistry
            .get(SimilarityTabOperator.class)
            .map(SimilarityTabOperator::getConfig)
            .orElseGet(() -> new SimilarityConfig.Builder().build());
    Sort sort =
        operatorRegistry.get(SortTabOperator.class).map(SortTabOperator::getSort).orElse(null);
    Set<String> fieldsToLoad =
        operatorRegistry
            .get(FieldValuesTabOperator.class)
            .map(FieldValuesTabOperator::getFieldsToLoad)
            .orElse(Collections.emptySet());
    SearchResults results =
        searchModel.search(
            query, simConfig, sort, fieldsToLoad, DEFAULT_PAGE_SIZE, exactHitsCntCB.isSelected());

    TableUtils.setupTable(
        resultsTable,
        ListSelectionModel.SINGLE_SELECTION,
        new SearchResultsTableModel(),
        null,
        SearchResultsTableModel.Column.DOCID.getColumnWidth(),
        SearchResultsTableModel.Column.SCORE.getColumnWidth());
    populateResults(results);

    messageBroker.clearStatusMessage();
  }

  private void nextPage() {
    searchModel.nextPage().ifPresent(this::populateResults);
    messageBroker.clearStatusMessage();
  }

  private void prevPage() {
    searchModel.prevPage().ifPresent(this::populateResults);
    messageBroker.clearStatusMessage();
  }

  private void doMLTSearch() {
    if (Objects.isNull(mltDocFTF.getValue())) {
      throw new LukeException("Doc num is not set.");
    }
    int docNum = (int) mltDocFTF.getValue();
    MLTConfig mltConfig =
        operatorRegistry
            .get(MLTTabOperator.class)
            .map(MLTTabOperator::getConfig)
            .orElseGet(() -> new MLTConfig.Builder().build());
    Analyzer analyzer =
        operatorRegistry
            .get(AnalysisTabOperator.class)
            .map(AnalysisTabOperator::getCurrentAnalyzer)
            .orElseGet(StandardAnalyzer::new);
    Query query = searchModel.mltQuery(docNum, mltConfig, analyzer);
    Set<String> fieldsToLoad =
        operatorRegistry
            .get(FieldValuesTabOperator.class)
            .map(FieldValuesTabOperator::getFieldsToLoad)
            .orElse(Collections.emptySet());
    SearchResults results =
        searchModel.search(
            query, new SimilarityConfig.Builder().build(), fieldsToLoad, DEFAULT_PAGE_SIZE, false);

    TableUtils.setupTable(
        resultsTable,
        ListSelectionModel.SINGLE_SELECTION,
        new SearchResultsTableModel(),
        null,
        SearchResultsTableModel.Column.DOCID.getColumnWidth(),
        SearchResultsTableModel.Column.SCORE.getColumnWidth());
    populateResults(results);

    messageBroker.clearStatusMessage();
  }

  private Query parse(boolean rewrite) {
    String expr =
        StringUtils.isNullOrEmpty(queryStringTA.getText()) ? "*:*" : queryStringTA.getText();
    String df =
        operatorRegistry
            .get(QueryParserTabOperator.class)
            .map(QueryParserTabOperator::getDefaultField)
            .orElse("");
    QueryParserConfig config =
        operatorRegistry
            .get(QueryParserTabOperator.class)
            .map(QueryParserTabOperator::getConfig)
            .orElseGet(() -> new QueryParserConfig.Builder().build());
    Analyzer analyzer =
        operatorRegistry
            .get(AnalysisTabOperator.class)
            .map(AnalysisTabOperator::getCurrentAnalyzer)
            .orElseGet(StandardAnalyzer::new);
    return searchModel.parseQuery(expr, df, analyzer, config, rewrite);
  }

  private void populateResults(SearchResults res) {
    totalHitsLbl.setText(String.valueOf(res.getTotalHits()));
    if (res.getTotalHits().value > 0) {
      startLbl.setText(String.valueOf(res.getOffset() + 1));
      endLbl.setText(String.valueOf(res.getOffset() + res.size()));

      prevBtn.setEnabled(res.getOffset() > 0);
      nextBtn.setEnabled(
          res.getTotalHits().relation == TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO
              || res.getTotalHits().value > res.getOffset() + res.size());

      if (!indexHandler.getState().readOnly() && indexHandler.getState().hasDirectoryReader()) {
        delBtn.setEnabled(true);
      }

      resultsTable.setModel(new SearchResultsTableModel(res));
      resultsTable
          .getColumnModel()
          .getColumn(SearchResultsTableModel.Column.DOCID.getIndex())
          .setPreferredWidth(SearchResultsTableModel.Column.DOCID.getColumnWidth());
      resultsTable
          .getColumnModel()
          .getColumn(SearchResultsTableModel.Column.SCORE.getIndex())
          .setPreferredWidth(SearchResultsTableModel.Column.SCORE.getColumnWidth());
      resultsTable
          .getColumnModel()
          .getColumn(SearchResultsTableModel.Column.VALUE.getIndex())
          .setPreferredWidth(SearchResultsTableModel.Column.VALUE.getColumnWidth());
    } else {
      startLbl.setText("0");
      endLbl.setText("0");
      prevBtn.setEnabled(false);
      nextBtn.setEnabled(false);
      delBtn.setEnabled(false);
    }
  }

  private void confirmDeletion() {
    if (Messages.showYesNoDialog(MessageUtils.getLocalizedMessage("search.message.delete_confirm"), "Confirm Deletion", Messages.getWarningIcon()) == Messages.YES) {
      deleteDocs();
    }
  }

  private void deleteDocs() {
    Query query = searchModel.getCurrentQuery();
    if (query != null) {
      toolsModel.deleteDocuments(query);
      indexHandler.reOpen();
      messageBroker.showStatusMessage(
          MessageUtils.getLocalizedMessage("search.message.delete_success", query.toString()));
    }
    delBtn.setEnabled(false);
  }

  private JPopupMenu setupResultsContextMenuPopup() {
    JPopupMenu popup = new JPopupMenu();

    // show explanation
    JMenuItem item1 =
        new JMenuItem(MessageUtils.getLocalizedMessage("search.results.menu.explain"));
    item1.addActionListener(
        e -> {
          int docid =
              (int)
                  resultsTable
                      .getModel()
                      .getValueAt(
                          resultsTable.getSelectedRow(),
                          SearchResultsTableModel.Column.DOCID.getIndex());
          Explanation explanation = searchModel.explain(parse(false), docid);
          new ExplainDialogFactory(project, docid, explanation).show();
        });
    popup.add(item1);

    // show all fields
    JMenuItem item2 =
        new JMenuItem(MessageUtils.getLocalizedMessage("search.results.menu.showdoc"));
    item2.addActionListener(
        e -> {
          int docid =
              (int)
                  resultsTable
                      .getModel()
                      .getValueAt(
                          resultsTable.getSelectedRow(),
                          SearchResultsTableModel.Column.DOCID.getIndex());
          operatorRegistry
              .get(DocumentsTabOperator.class)
              .ifPresent(operator -> operator.displayDoc(docid));
          tabSwitcher.switchTab(TabbedPaneProvider.Tab.DOCUMENTS);
        });
    popup.add(item2);

    return popup;
  }

  @Override
  public void searchByTerm(String field, String term) {
    termQueryCB.setSelected(true);
    enableTermQuery();
    queryStringTA.setText(field + ":" + term);
    doSearch();
  }

  @Override
  public void mltSearch(int docNum) {
    mltDocFTF.setValue(docNum);
    doMLTSearch();
    tabbedPane.setSelectedIndex(Tab.MLT.index());
  }

  @Override
  public void enableExactHitsCB(boolean value) {
    exactHitsCntCB.setEnabled(value);
  }

  @Override
  public void setExactHits(boolean value) {
    exactHitsCntCB.setSelected(value);
  }

  private class ListenerFunctions {

    void toggleTermQuery(ActionEvent e) {
      SearchPanelProvider.this.toggleTermQuery();
    }

    void execParse(ActionEvent e) {
      SearchPanelProvider.this.execParse();
    }

    void execSearch(ActionEvent e) {
      SearchPanelProvider.this.doSearch();
    }

    void nextPage(ActionEvent e) {
      SearchPanelProvider.this.nextPage();
    }

    void prevPage(ActionEvent e) {
      SearchPanelProvider.this.prevPage();
    }

    void execMLTSearch(ActionEvent e) {
      SearchPanelProvider.this.doMLTSearch();
    }

    void confirmDeletion(ActionEvent e) {
      SearchPanelProvider.this.confirmDeletion();
    }

    void showContextMenuInResultsTable(MouseEvent e) {
      if (e.getClickCount() == 2 && !e.isConsumed()) {
        SearchPanelProvider.this
            .setupResultsContextMenuPopup()
            .show(e.getComponent(), e.getX(), e.getY());
        setupResultsContextMenuPopup().show(e.getComponent(), e.getX(), e.getY());
      }
    }
  }

  private class Observer implements IndexObserver {

    @Override
    public void openIndex(LukeState state) {
      searchModel = searchFactory.newInstance(state.getIndexReader());
      toolsModel =
          toolsFactory.newInstance(
              state.getIndexReader(), state.useCompound(), state.keepAllCommits());
      operatorRegistry
          .get(QueryParserTabOperator.class)
          .ifPresent(
              operator -> {
                operator.setSearchableFields(searchModel.getSearchableFieldNames());
                operator.setRangeSearchableFields(searchModel.getRangeSearchableFieldNames());
              });
      operatorRegistry
          .get(SortTabOperator.class)
          .ifPresent(
              operator -> {
                operator.setSearchModel(searchModel);
                operator.setSortableFields(searchModel.getSortableFieldNames());
              });
      operatorRegistry
          .get(FieldValuesTabOperator.class)
          .ifPresent(operator -> operator.setFields(searchModel.getFieldNames()));
      operatorRegistry
          .get(MLTTabOperator.class)
          .ifPresent(operator -> operator.setFields(searchModel.getFieldNames()));

      queryStringTA.setText("*:*");
      parsedQueryTA.setText("");
      parseBtn.setEnabled(true);
      searchBtn.setEnabled(true);
      mltBtn.setEnabled(true);
    }

    @Override
    public void closeIndex() {
      searchModel = null;
      toolsModel = null;

      queryStringTA.setText("");
      parsedQueryTA.setText("");
      parseBtn.setEnabled(false);
      searchBtn.setEnabled(false);
      mltBtn.setEnabled(false);
      totalHitsLbl.setText("0");
      startLbl.setText("0");
      endLbl.setText("0");
      nextBtn.setEnabled(false);
      prevBtn.setEnabled(false);
      delBtn.setEnabled(false);
      TableUtils.setupTable(
          resultsTable,
          ListSelectionModel.SINGLE_SELECTION,
          new SearchResultsTableModel(),
          null,
          SearchResultsTableModel.Column.DOCID.getColumnWidth(),
          SearchResultsTableModel.Column.SCORE.getColumnWidth());
    }
  }

  /** tabs in the Search panel */
  public enum Tab {
    QPARSER(0),
    ANALYZER(1),
    SIMILARITY(2),
    SORT(3),
    VALUES(4),
    MLT(5);

    private final int tabIdx;

    Tab(int tabIdx) {
      this.tabIdx = tabIdx;
    }

    int index() {
      return tabIdx;
    }
  }

  static final class SearchResultsTableModel
      extends TableModelBase<SearchResultsTableModel.Column> {

    enum Column implements TableColumnInfo {
      DOCID("Doc ID", 0, Integer.class, 50),
      SCORE("Score", 1, Float.class, 100),
      VALUE("Field Values", 2, String.class, 800);

      private final String colName;
      private final int index;
      private final Class<?> type;
      private final int width;

      Column(String colName, int index, Class<?> type, int width) {
        this.colName = colName;
        this.index = index;
        this.type = type;
        this.width = width;
      }

      @Override
      public String getColName() {
        return colName;
      }

      @Override
      public int getIndex() {
        return index;
      }

      @Override
      public Class<?> getType() {
        return type;
      }

      @Override
      public int getColumnWidth() {
        return width;
      }
    }

    SearchResultsTableModel() {
      super();
    }

    SearchResultsTableModel(SearchResults results) {
      super(results.size());
      for (int i = 0; i < results.size(); i++) {
        SearchResults.Doc doc = results.getHits().get(i);
        data[i][Column.DOCID.getIndex()] = doc.getDocId();
        if (!Float.isNaN(doc.getScore())) {
          data[i][Column.SCORE.getIndex()] = doc.getScore();
        } else {
          data[i][Column.SCORE.getIndex()] = 1.0f;
        }
        List<String> concatValues =
            doc.getFieldValues().entrySet().stream()
                .map(
                    e -> {
                      String v = String.join(",", Arrays.asList(e.getValue()));
                      return e.getKey() + "=" + v + ";";
                    })
                .toList();
        data[i][Column.VALUE.getIndex()] = String.join(" ", concatValues);
      }
    }

    @Override
    protected Column[] columnInfos() {
      return Column.values();
    }
  }
}
