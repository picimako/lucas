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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import org.apache.lucene.luke.app.desktop.components.TableColumnInfo;
import org.apache.lucene.luke.app.desktop.components.TableModelBase;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.TableUtils;
import org.apache.lucene.luke.models.documents.TermVectorEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Factory of term vector dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.documents.TermVectorDialogFactory}.
 */
public final class TermVectorDialogFactory extends DialogWrapper {

  private final String field;

  private final List<TermVectorEntry> tvEntries;

  public TermVectorDialogFactory(@Nullable Project project, String field, List<TermVectorEntry> tvEntries) {
    super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);

    if (Objects.isNull(field) || Objects.isNull(tvEntries)) {
      throw new IllegalStateException("field name and/or term vector is not set.");
    }

    this.field = field;
    this.tvEntries = tvEntries;

    setTitle("Term Vector");
    setSize(600, 400);
    setCancelButtonText(MessageUtils.getLocalizedMessage("button.close"));

    init();
  }

  @Override
  protected Action @NotNull [] createActions() {
    return new Action[]{getCancelAction()};
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
    header.add(
        new JBLabel(MessageUtils.getLocalizedMessage("documents.termvector.label.term_vector")));
    header.add(new JLabel(field));
    panel.add(header, BorderLayout.PAGE_START);

    JTable tvTable = new JBTable();
    TableUtils.setupTable(
        tvTable,
        ListSelectionModel.SINGLE_SELECTION,
        new TermVectorTableModel(tvEntries),
        null,
        100,
        50,
        100);
    JScrollPane scrollPane = new JBScrollPane(tvTable);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  static final class TermVectorTableModel extends TableModelBase<TermVectorTableModel.Column> {

    enum Column implements TableColumnInfo {
      TERM("Term", 0, String.class),
      FREQ("Freq", 1, Long.class),
      POSITIONS("Positions", 2, String.class),
      OFFSETS("Offsets", 3, String.class);

      private final String colName;
      private final int index;
      private final Class<?> type;

      Column(String colName, int index, Class<?> type) {
        this.colName = colName;
        this.index = index;
        this.type = type;
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
    }

    TermVectorTableModel() {
      super();
    }

    TermVectorTableModel(List<TermVectorEntry> tvEntries) {
      super(tvEntries.size());

      for (int i = 0; i < tvEntries.size(); i++) {
        TermVectorEntry entry = tvEntries.get(i);

        String termText = entry.getTermText();
        long freq = tvEntries.get(i).getFreq();
        String positions =
            entry.getPositions().stream()
                .map(pos -> Integer.toString(pos.getPosition()))
                .collect(Collectors.joining(","));
        String offsets =
            entry.getPositions().stream()
                .filter(pos -> pos.getStartOffset().isPresent() && pos.getEndOffset().isPresent())
                .map(pos -> pos.getStartOffset().orElse(-1) + "-" + pos.getEndOffset().orElse(-1))
                .collect(Collectors.joining(","));

        data[i] = new Object[] {termText, freq, positions, offsets};
      }
    }

    @Override
    protected Column[] columnInfos() {
      return Column.values();
    }
  }
}
