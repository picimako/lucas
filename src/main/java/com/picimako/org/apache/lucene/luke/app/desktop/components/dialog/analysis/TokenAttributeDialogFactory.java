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
import org.apache.lucene.luke.app.desktop.components.TableColumnInfo;
import org.apache.lucene.luke.app.desktop.components.TableModelBase;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.TableUtils;
import org.apache.lucene.luke.models.analysis.Analysis;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Factory of token attribute dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.analysis.TokenAttributeDialogFactory}.
 */
public final class TokenAttributeDialogFactory extends DialogWrapper {

  private final JTable attributesTable = new JBTable();

  private final String term;

  private final List<Analysis.TokenAttribute> attributes;

  public TokenAttributeDialogFactory(Project project, String term, List<Analysis.TokenAttribute> attributes) {
    super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);
    this.term = term;
    this.attributes = attributes;

    setTitle("Token Attributes");
    setSize(650, 400);
    setOKButtonText(MessageUtils.getLocalizedMessage("button.ok"));

    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return content();
  }

  private JPanel content() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEADING));
    header.setOpaque(false);
    header.add(new JLabel("All token attributes for:"));
    header.add(new JLabel(term));
    panel.add(header, BorderLayout.PAGE_START);

    List<TokenAttValue> attrValues =
        attributes.stream()
            .flatMap(
                att ->
                    att.getAttValues().entrySet().stream()
                        .map(e -> TokenAttValue.of(att.getAttClass(), e.getKey(), e.getValue())))
            .toList();
    TableUtils.setupTable(
        attributesTable,
        ListSelectionModel.SINGLE_SELECTION,
        new AttributeTableModel(attrValues),
        null);
    panel.add(new JScrollPane(attributesTable), BorderLayout.CENTER);

    return panel;
  }

  static final class AttributeTableModel extends TableModelBase<AttributeTableModel.Column> {

    enum Column implements TableColumnInfo {
      ATTR("Attribute", 0, String.class),
      NAME("Name", 1, String.class),
      VALUE("Value", 2, String.class);

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

    AttributeTableModel(List<TokenAttValue> attrValues) {
      super(attrValues.size());
      for (int i = 0; i < attrValues.size(); i++) {
        TokenAttValue attrValue = attrValues.get(i);
        data[i][Column.ATTR.getIndex()] = attrValue.getAttClass();
        data[i][Column.NAME.getIndex()] = attrValue.getName();
        data[i][Column.VALUE.getIndex()] = attrValue.getValue();
      }
    }

    @Override
    protected Column[] columnInfos() {
      return Column.values();
    }
  }

  static final class TokenAttValue {
    private String attClass;
    private String name;
    private String value;

    public static TokenAttValue of(String attClass, String name, String value) {
      TokenAttValue attValue = new TokenAttValue();
      attValue.attClass = attClass;
      attValue.name = name;
      attValue.value = value;
      return attValue;
    }

    private TokenAttValue() {}

    String getAttClass() {
      return attClass;
    }

    String getName() {
      return name;
    }

    String getValue() {
      return value;
    }
  }
}
