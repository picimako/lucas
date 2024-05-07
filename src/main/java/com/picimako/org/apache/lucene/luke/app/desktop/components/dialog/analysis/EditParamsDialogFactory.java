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
import org.apache.lucene.luke.app.desktop.components.dialog.analysis.EditParamsMode;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.TableUtils;
import org.apache.lucene.luke.app.desktop.util.lang.Callable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Factory of edit parameters dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.analysis.EditParamsDialogFactory}.
 */
public final class EditParamsDialogFactory extends DialogWrapper {

  private final ComponentOperatorRegistry operatorRegistry;

  private final JTable paramsTable = new JBTable();

  private EditParamsMode mode;

  private final String target;

  private int targetIndex;

  private final Map<String, String> params;

  private Callable callback;

  public EditParamsDialogFactory(Project project, String title, String target, Map<String, String> params) {
    super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);

    this.operatorRegistry = ComponentOperatorRegistry.getInstance();
    this.target = target;
    this.params = params;

    setTitle(title);
    setSize(400, 300);
    setOKButtonText(MessageUtils.getLocalizedMessage("button.ok"));
    setCancelButtonText(MessageUtils.getLocalizedMessage("button.cancel"));

    init();
  }

  @Override
  protected void doOKAction() {
    Map<String, String> params = new HashMap<>();
    for (int i = 0; i < paramsTable.getRowCount(); i++) {
      boolean deleted =
          (boolean) paramsTable.getValueAt(i, ParamsTableModel.Column.DELETE.getIndex());
      String name =
          (String) paramsTable.getValueAt(i, ParamsTableModel.Column.NAME.getIndex());
      String value =
          (String) paramsTable.getValueAt(i, ParamsTableModel.Column.VALUE.getIndex());
      if (deleted
          || Objects.isNull(name)
          || name.isEmpty()
          || Objects.isNull(value)
          || value.isEmpty()) {
        continue;
      }
      params.put(name, value);
    }
    updateTargetParams(params);
    callback.call();
    this.params.clear();
    super.doOKAction();
  }

  @Override
  public void doCancelAction() {
    this.params.clear();
    super.doCancelAction();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return content();
  }

  public void setMode(EditParamsMode mode) {
    this.mode = mode;
  }

  public void setTargetIndex(int targetIndex) {
    this.targetIndex = targetIndex;
  }

  public void setCallback(Callable callback) {
    this.callback = callback;
  }

  private JPanel content() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 10));
    header.setOpaque(false);
    header.add(new JLabel("Parameters for:"));
    String[] tmp = target.split("\\.");
    JLabel targetLbl = new JLabel(tmp[tmp.length - 1]);
    header.add(targetLbl);
    panel.add(header, BorderLayout.PAGE_START);

    TableUtils.setupTable(
        paramsTable,
        ListSelectionModel.SINGLE_SELECTION,
        new ParamsTableModel(params),
        null,
        ParamsTableModel.Column.DELETE.getColumnWidth(),
        ParamsTableModel.Column.NAME.getColumnWidth());
    paramsTable.setShowGrid(true);
    panel.add(new JScrollPane(paramsTable), BorderLayout.CENTER);

    return panel;
  }

  private void updateTargetParams(Map<String, String> params) {
    operatorRegistry
        .get(CustomAnalyzerPanelOperator.class)
        .ifPresent(
            operator -> {
              switch (mode) {
                case CHARFILTER:
                  operator.updateCharFilterParams(targetIndex, params);
                  break;
                case TOKENIZER:
                  operator.updateTokenizerParams(params);
                  break;
                case TOKENFILTER:
                  operator.updateTokenFilterParams(targetIndex, params);
                  break;
              }
            });
  }

  static final class ParamsTableModel extends TableModelBase<ParamsTableModel.Column> {

    enum Column implements TableColumnInfo {
      DELETE("Delete", 0, Boolean.class, 50),
      NAME("Name", 1, String.class, 150),
      VALUE("Value", 2, String.class, Integer.MAX_VALUE);

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

    private static final int PARAM_SIZE = 20;

    ParamsTableModel(Map<String, String> params) {
      super(PARAM_SIZE);
      List<String> keys = new ArrayList<>(params.keySet());
      for (int i = 0; i < keys.size(); i++) {
        data[i][Column.NAME.getIndex()] = keys.get(i);
        data[i][Column.VALUE.getIndex()] = params.get(keys.get(i));
      }
      for (int i = 0; i < data.length; i++) {
        data[i][Column.DELETE.getIndex()] = false;
      }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return true;
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
}
