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
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.models.documents.DocValues;
import org.apache.lucene.luke.util.BytesRefUtils;
import org.apache.lucene.util.NumericUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Factory of doc values dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.documents.DocValuesDialogFactory}.
 */
public final class DocValuesDialogFactory extends DialogWrapper {

  private final JComboBox<String> decodersCombo = new ComboBox<>();

  private final JList<String> valueList = new JBList<>();

  private final ListenerFunctions listeners = new ListenerFunctions();

  private String field;

  private DocValues docValues;

  public DocValuesDialogFactory(@Nullable Project project, String field, DocValues docValues) {
    super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);

    if (Objects.isNull(field) || Objects.isNull(docValues)) {
      throw new IllegalStateException("field name and/or doc values is not set.");
    }

    setValue(field, docValues);
    setTitle("Doc Values");
    setSize(400, 300);

    setOKButtonText(MessageUtils.getLocalizedMessage("button.copy"));
    setOKButtonIcon(AllIcons.Actions.Copy);
    setCancelButtonText(MessageUtils.getLocalizedMessage("button.close"));

    init();
  }

  @Override
  protected void doOKAction() {
    listeners.copyValues();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return content();
  }

  public void setValue(String field, DocValues docValues) {
    this.field = field;
    this.docValues = docValues;

    DefaultListModel<String> values = new DefaultListModel<>();
    if (docValues.getValues().size() > 0) {
      decodersCombo.setEnabled(false);
      docValues.getValues().stream().map(BytesRefUtils::decode).forEach(values::addElement);
    } else if (docValues.getNumericValues().size() > 0) {
      decodersCombo.setEnabled(true);
      docValues.getNumericValues().stream().map(String::valueOf).forEach(values::addElement);
    }

    valueList.setModel(values);
  }

  private JPanel content() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    panel.add(headerPanel(), BorderLayout.PAGE_START);
    JScrollPane scrollPane = new JBScrollPane(valueList());
    scrollPane.setOpaque(false);
    scrollPane.getViewport().setOpaque(false);
    panel.add(scrollPane, BorderLayout.CENTER);
    return panel;
  }

  private JPanel headerPanel() {
    JPanel header = new JPanel();
    header.setOpaque(false);
    header.setLayout(new BoxLayout(header, BoxLayout.PAGE_AXIS));

    JPanel fieldHeader = new JPanel(new FlowLayout(FlowLayout.LEADING, 3, 3));
    fieldHeader.setOpaque(false);
    fieldHeader.add(
        new JLabel(MessageUtils.getLocalizedMessage("documents.docvalues.label.doc_values")));
    fieldHeader.add(new JLabel(field));
    header.add(fieldHeader);

    JPanel typeHeader = new JPanel(new FlowLayout(FlowLayout.LEADING, 3, 3));
    typeHeader.setOpaque(false);
    typeHeader.add(new JLabel(MessageUtils.getLocalizedMessage("documents.docvalues.label.type")));
    typeHeader.add(new JLabel(docValues.getDvType().toString()));
    header.add(typeHeader);

    JPanel decodeHeader = new JPanel(new FlowLayout(FlowLayout.TRAILING, 3, 3));
    decodeHeader.setOpaque(false);
    decodeHeader.add(new JLabel("decoded as"));
    String[] decoders =
        Arrays.stream(Decoder.values()).map(Decoder::toString).toArray(String[]::new);
    decodersCombo.setModel(new DefaultComboBoxModel<>(decoders));
    decodersCombo.setSelectedItem(Decoder.LONG.toString());
    decodersCombo.addActionListener(listeners::selectDecoder);
    decodeHeader.add(decodersCombo);
    if (docValues.getValues().size() > 0) {
      decodeHeader.setEnabled(false);
    }
    header.add(decodeHeader);

    return header;
  }

  private JList<String> valueList() {
    valueList.setVisibleRowCount(5);
    valueList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    valueList.setLayoutOrientation(JList.VERTICAL);

    DefaultListModel<String> values = new DefaultListModel<>();
    if (docValues.getValues().size() > 0) {
      docValues.getValues().stream().map(BytesRefUtils::decode).forEach(values::addElement);
    } else {
      docValues.getNumericValues().stream().map(String::valueOf).forEach(values::addElement);
    }
    valueList.setModel(values);

    return valueList;
  }

  // control methods

  private void selectDecoder() {
    String decoderLabel = (String) decodersCombo.getSelectedItem();
    Decoder decoder = Decoder.fromLabel(decoderLabel);

    if (docValues.getNumericValues().isEmpty()) {
      return;
    }

    DefaultListModel<String> values = new DefaultListModel<>();
    switch (decoder) {
      case LONG:
        docValues.getNumericValues().stream().map(String::valueOf).forEach(values::addElement);
        break;
      case FLOAT:
        docValues.getNumericValues().stream()
            .mapToInt(Long::intValue)
            .mapToObj(NumericUtils::sortableIntToFloat)
            .map(String::valueOf)
            .forEach(values::addElement);
        break;
      case DOUBLE:
        docValues.getNumericValues().stream()
            .map(NumericUtils::sortableLongToDouble)
            .map(String::valueOf)
            .forEach(values::addElement);
        break;
    }

    valueList.setModel(values);
  }

  private void copyValues() {
    List<String> values = valueList.getSelectedValuesList();
    if (values.isEmpty()) {
      values = getAllVlues();
    }

    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    StringSelection selection = new StringSelection(String.join("\n", values));
    clipboard.setContents(selection, null);
  }

  private List<String> getAllVlues() {
    List<String> values = new ArrayList<>();
    for (int i = 0; i < valueList.getModel().getSize(); i++) {
      values.add(valueList.getModel().getElementAt(i));
    }
    return values;
  }

  private class ListenerFunctions {

    void selectDecoder(ActionEvent e) {
      DocValuesDialogFactory.this.selectDecoder();
    }

    void copyValues() {
      DocValuesDialogFactory.this.copyValues();
    }
  }

  /** doc value decoders */
  public enum Decoder {
    LONG("long"),
    FLOAT("float"),
    DOUBLE("double");

    private final String label;

    Decoder(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }

    public static Decoder fromLabel(String label) {
      for (Decoder d : values()) {
        if (d.label.equalsIgnoreCase(label)) {
          return d;
        }
      }
      throw new IllegalArgumentException("No such decoder: " + label);
    }
  }
}
