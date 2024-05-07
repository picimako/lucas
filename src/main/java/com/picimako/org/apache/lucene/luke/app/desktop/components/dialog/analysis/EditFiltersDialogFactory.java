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
import com.intellij.ui.table.JBTable;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import com.picimako.org.apache.lucene.luke.app.desktop.components.ComponentOperatorRegistry;
import com.picimako.org.apache.lucene.luke.app.desktop.components.fragments.analysis.CustomAnalyzerPanelOperator;
import org.apache.lucene.luke.app.desktop.components.TableColumnInfo;
import org.apache.lucene.luke.app.desktop.components.TableModelBase;
import org.apache.lucene.luke.app.desktop.components.dialog.analysis.EditFiltersMode;
import org.apache.lucene.luke.app.desktop.components.dialog.analysis.EditParamsMode;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.TableUtils;
import org.apache.lucene.luke.app.desktop.util.lang.Callable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Factory of edit filters dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.analysis.EditFiltersDialogFactory}.
 */
public final class EditFiltersDialogFactory extends DialogWrapper {

  private final ComponentOperatorRegistry operatorRegistry;

  private final JLabel targetLbl = new JLabel();

  private final JTable filtersTable = new JBTable();

  private final ListenerFunctions listeners = new ListenerFunctions();

  private final FiltersTableMouseListener tableListener;

  private final List<String> selectedFilters;

  private final Callable callback;

  private final EditFiltersMode mode;

  public EditFiltersDialogFactory(@Nullable Project project, String title, List<String> selectedFilters, Callable callback, EditFiltersMode mode) {
    super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);
    this.selectedFilters = selectedFilters;
    this.callback = callback;
    this.mode = mode;

    this.operatorRegistry = ComponentOperatorRegistry.getInstance();
    this.tableListener = new FiltersTableMouseListener(project);

    setTitle(title);
    setSize(400, 300);
    setOKButtonText(MessageUtils.getLocalizedMessage("button.ok"));
    setCancelButtonText(MessageUtils.getLocalizedMessage("button.cancel"));

    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return content();
  }

  @Override
  protected void doOKAction() {
    List<Integer> deletedIndexes = new ArrayList<>();
    for (int i = 0; i < filtersTable.getRowCount(); i++) {
      boolean deleted =
          (boolean) filtersTable.getValueAt(i, FiltersTableModel.Column.DELETE.getIndex());
      if (deleted) {
        deletedIndexes.add(i);
      }
    }
    operatorRegistry
        .get(CustomAnalyzerPanelOperator.class)
        .ifPresent(
            operator -> {
              switch (mode) {
                case CHARFILTER:
                  operator.updateCharFilters(deletedIndexes);
                  break;
                case TOKENFILTER:
                  operator.updateTokenFilters(deletedIndexes);
                  break;
              }
            });
    callback.call();

    super.doOKAction();
  }

  private JPanel content() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 10));
    header.setOpaque(false);
    header.add(new JLabel(MessageUtils.getLocalizedMessage("analysis.dialog.hint.edit_param")));
    header.add(targetLbl);
    panel.add(header, BorderLayout.PAGE_START);

    TableUtils.setupTable(
        filtersTable,
        ListSelectionModel.SINGLE_SELECTION,
        new FiltersTableModel(selectedFilters),
        tableListener,
        FiltersTableModel.Column.DELETE.getColumnWidth(),
        FiltersTableModel.Column.ORDER.getColumnWidth());
    filtersTable.setShowGrid(true);
    filtersTable
        .getColumnModel()
        .getColumn(FiltersTableModel.Column.TYPE.getIndex())
        .setCellRenderer(new TypeCellRenderer());
    panel.add(new JScrollPane(filtersTable), BorderLayout.CENTER);

    return panel;
  }

  private class ListenerFunctions {

    void showEditParamsDialog(MouseEvent e, Project project) {
      if (e.getClickCount() != 2 || e.isConsumed()) {
        return;
      }
      int selectedIndex = filtersTable.rowAtPoint(e.getPoint());
      if (selectedIndex < 0 || selectedIndex >= selectedFilters.size()) {
        return;
      }

      switch (mode) {
        case CHARFILTER:
          showEditParamsCharFilterDialog(selectedIndex, project);
          break;
        case TOKENFILTER:
          showEditParamsTokenFilterDialog(selectedIndex, project);
          break;
        default:
      }
    }

    private void showEditParamsCharFilterDialog(int selectedIndex, Project project) {
      int targetIndex = filtersTable.getSelectedRow();
      String selectedItem =
          (String) filtersTable.getValueAt(selectedIndex, FiltersTableModel.Column.TYPE.getIndex());
      Map<String, String> params =
          operatorRegistry
              .get(CustomAnalyzerPanelOperator.class)
              .map(operator -> operator.getCharFilterParams(targetIndex))
              .orElse(Collections.emptyMap());
      EditParamsDialogFactory factory = new EditParamsDialogFactory(project, MessageUtils.getLocalizedMessage("analysis.dialog.title.char_filter_params"), selectedItem, params);
      factory.setMode(EditParamsMode.CHARFILTER);
      factory.setTargetIndex(targetIndex);
      factory.show();
    }

    private void showEditParamsTokenFilterDialog(int selectedIndex, Project project) {
      int targetIndex = filtersTable.getSelectedRow();
      String selectedItem =
          (String) filtersTable.getValueAt(selectedIndex, FiltersTableModel.Column.TYPE.getIndex());
      Map<String, String> params =
          operatorRegistry
              .get(CustomAnalyzerPanelOperator.class)
              .map(operator -> operator.getTokenFilterParams(targetIndex))
              .orElse(Collections.emptyMap());
      EditParamsDialogFactory factory = new EditParamsDialogFactory(project, MessageUtils.getLocalizedMessage("analysis.dialog.title.char_filter_params"), selectedItem, params);
      factory.setMode(EditParamsMode.TOKENFILTER);
      factory.setTargetIndex(targetIndex);
      factory.show();
    }
  }

  private class FiltersTableMouseListener extends MouseAdapter {

    private final Project project;

    public FiltersTableMouseListener(Project project) {
      this.project = project;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      listeners.showEditParamsDialog(e, project);
    }
  }

  static final class FiltersTableModel extends TableModelBase<FiltersTableModel.Column> {

    enum Column implements TableColumnInfo {
      DELETE("Delete", 0, Boolean.class, 50),
      ORDER("Order", 1, Integer.class, 50),
      TYPE("Factory class", 2, String.class, Integer.MAX_VALUE);

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

    FiltersTableModel() {
      super();
    }

    FiltersTableModel(List<String> selectedFilters) {
      super(selectedFilters.size());
      for (int i = 0; i < selectedFilters.size(); i++) {
        data[i][Column.DELETE.getIndex()] = false;
        data[i][Column.ORDER.getIndex()] = i + 1;
        data[i][Column.TYPE.getIndex()] = selectedFilters.get(i);
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == Column.DELETE.getIndex();
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
      data[rowIndex][columnIndex] = value;
    }

    @Override
    protected Column[] columnInfos() {
      return Column.values();
    }
  }

  static final class TypeCellRenderer implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      String[] tmp = ((String) value).split("\\.");
      String type = tmp[tmp.length - 1];
      return new JLabel(type);
    }
  }
}
