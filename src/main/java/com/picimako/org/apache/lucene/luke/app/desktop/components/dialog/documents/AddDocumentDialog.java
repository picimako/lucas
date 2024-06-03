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
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import com.picimako.org.apache.lucene.luke.app.desktop.components.AnalysisTabOperator;
import com.picimako.org.apache.lucene.luke.app.desktop.components.ComponentOperatorRegistry;
import com.picimako.org.apache.lucene.luke.app.desktop.components.DocumentsTabOperator;
import com.picimako.org.apache.lucene.luke.app.desktop.components.TabSwitcherProxy;
import com.picimako.org.apache.lucene.luke.app.desktop.components.TabbedPaneProvider;
import com.picimako.org.apache.lucene.luke.app.desktop.util.HelpHeaderRenderer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.luke.app.desktop.components.TableColumnInfo;
import org.apache.lucene.luke.app.desktop.components.TableModelBase;
import org.apache.lucene.luke.app.desktop.dto.documents.NewField;
import org.apache.lucene.luke.app.desktop.util.FontUtils;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.NumericUtils;
import org.apache.lucene.luke.app.desktop.util.StringUtils;
import org.apache.lucene.luke.app.desktop.util.TableUtils;
import org.apache.lucene.luke.models.LukeException;
import org.apache.lucene.luke.models.tools.IndexTools;
import org.apache.lucene.luke.util.LoggerFactory;
import org.apache.lucene.util.BytesRef;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * LICENSE NOTE: This class is extracted from {@link org.apache.lucene.luke.app.desktop.components.dialog.documents.AddDocumentDialogFactory}.
 */
public class AddDocumentDialog extends DialogWrapper {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int ROW_COUNT = 50;

    private final IndexHandler indexHandler;

    private final TabSwitcherProxy tabSwitcher;

    private final ComponentOperatorRegistry operatorRegistry;

    private final ListenerFunctions listeners = new ListenerFunctions();

    private final JLabel analyzerNameLbl = new JLabel();

    private final List<NewField> newFieldList;

    private final JTextArea infoTA = new JTextArea();

    private IndexTools toolsModel;

    private final Project project;

    public AddDocumentDialog(@Nullable Project project, IndexTools toolsModel, @Nullable("When there is no specific analyzer set apart from StandardAnalyzer.") Analyzer analyzer) {
        super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);
        this.project = project;
        this.toolsModel = toolsModel;
        this.analyzerNameLbl.setText(analyzer != null ? analyzer.getClass().getName() : StandardAnalyzer.class.getName());
        this.indexHandler = IndexHandler.getInstance();
        this.tabSwitcher = TabSwitcherProxy.getInstance();
        this.operatorRegistry = ComponentOperatorRegistry.getInstance();
        this.newFieldList =
            IntStream.range(0, ROW_COUNT).mapToObj(i -> NewField.newInstance()).toList();

        setTitle("Add document");
        setSize(600, 500);

        setOKButtonText(MessageUtils.getLocalizedMessage("add_document.button.add"));
        setOKActionEnabled(true);
        setCancelButtonText(MessageUtils.getLocalizedMessage("button.cancel"));

        initialize();
        init();
    }

    private void initialize() {
        infoTA.setRows(3);
        infoTA.setLineWrap(true);
        infoTA.setEditable(false);
        infoTA.setText(MessageUtils.getLocalizedMessage("add_document.info"));
        infoTA.setForeground(JBColor.GRAY);
    }

    @Override
    protected void doOKAction() {
        listeners.addDocument();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return content();
    }

    private JPanel content() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.add(header(), BorderLayout.PAGE_START);
        panel.add(center(), BorderLayout.CENTER);
        panel.add(footer(), BorderLayout.PAGE_END);
        return panel;
    }

    private JPanel header() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        JPanel analyzerHeader = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 10));
        analyzerHeader.setOpaque(false);
        analyzerHeader.add(new JLabel(MessageUtils.getLocalizedMessage("add_document.label.analyzer")));
        analyzerHeader.add(analyzerNameLbl);
        JLabel changeLbl =
            new JLabel(MessageUtils.getLocalizedMessage("add_document.hyperlink.change"));
        changeLbl.addMouseListener(
            new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    tabSwitcher.switchTab(TabbedPaneProvider.Tab.ANALYZER);
                    close(DialogWrapper.OK_EXIT_CODE);
                }
            });
        analyzerHeader.add(FontUtils.toLinkText(changeLbl));
        panel.add(analyzerHeader);

        return panel;
    }

    private JPanel center() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(new TitledSeparator(MessageUtils.getLocalizedMessage("add_document.label.fields")), BorderLayout.PAGE_START);

        JScrollPane scrollPane = new JBScrollPane(fieldsTable());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JTable fieldsTable() {
        JTable fieldsTable = new JBTable();
        TableUtils.setupTable(
            fieldsTable,
            ListSelectionModel.SINGLE_SELECTION,
            new FieldsTableModel(newFieldList),
            null,
            30,
            150,
            120,
            80);
        fieldsTable.setShowGrid(true);
        JComboBox<Class<? extends IndexableField>> typesCombo = new ComboBox<>(presetFieldClasses);
        typesCombo.setRenderer(
            (list, value, index, isSelected, cellHasFocus) -> new JBLabel(value.getSimpleName()));
        fieldsTable
            .getColumnModel()
            .getColumn(FieldsTableModel.Column.TYPE.getIndex())
            .setCellEditor(new DefaultCellEditor(typesCombo));
        for (int i = 0; i < fieldsTable.getModel().getRowCount(); i++) {
            fieldsTable
                .getModel()
                .setValueAt(TextField.class, i, FieldsTableModel.Column.TYPE.getIndex());
        }
        fieldsTable
            .getColumnModel()
            .getColumn(FieldsTableModel.Column.TYPE.getIndex())
            .setHeaderRenderer(
                new HelpHeaderRenderer(
                    "About Type",
                    "Select Field Class:",
                    createTypeHelpDialog(),
                    project,
                    getContentPanel()));
        fieldsTable
            .getColumnModel()
            .getColumn(FieldsTableModel.Column.TYPE.getIndex())
            .setCellRenderer(new TypeCellRenderer());
        fieldsTable
            .getColumnModel()
            .getColumn(FieldsTableModel.Column.OPTIONS.getIndex())
            .setCellRenderer(new OptionsCellRenderer(project, getContentPanel(), newFieldList));
        return fieldsTable;
    }

    private JComponent createTypeHelpDialog() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JTextArea descTA = new JTextArea();

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.PAGE_AXIS));
        String[] typeList =
            new String[]{
                "TextField",
                "StringField",
                "IntPoint",
                "LongPoint",
                "FloatPoint",
                "DoublePoint",
                "SortedDocValuesField",
                "SortedSetDocValuesField",
                "NumericDocValuesField",
                "SortedNumericDocValuesField",
                "StoredField",
                "Field"
            };
        JPanel wrapper1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        wrapper1.setOpaque(false);
        JComboBox<String> typeCombo = new ComboBox<>(typeList);
        typeCombo.setSelectedItem(typeList[0]);
        typeCombo.addActionListener(
            e -> {
                String selected = (String) typeCombo.getSelectedItem();
                descTA.setText(MessageUtils.getLocalizedMessage("help.fieldtype." + selected));
            });
        wrapper1.add(typeCombo);
        header.add(wrapper1);
        JPanel wrapper2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        wrapper2.setOpaque(false);
        wrapper2.add(new JLabel("Brief description and Examples"));
        header.add(wrapper2);
        panel.add(header, BorderLayout.PAGE_START);

        descTA.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        descTA.setEditable(false);
        descTA.setLineWrap(true);
        descTA.setRows(10);
        descTA.setText(MessageUtils.getLocalizedMessage("help.fieldtype." + typeList[0]));
        JScrollPane scrollPane = new JBScrollPane(descTA);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel footer() {
        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JBScrollPane(infoTA);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        panel.add(scrollPane);
        return panel;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private final Class<? extends IndexableField>[] presetFieldClasses =
        new Class[]{
            TextField.class,
            StringField.class,
            IntPoint.class,
            LongPoint.class,
            FloatPoint.class,
            DoublePoint.class,
            SortedDocValuesField.class,
            SortedSetDocValuesField.class,
            NumericDocValuesField.class,
            SortedNumericDocValuesField.class,
            StoredField.class,
            Field.class
        };

    private class ListenerFunctions {

        void addDocument() {
            List<NewField> validFields =
                newFieldList.stream()
                    .filter(nf -> !nf.isDeleted())
                    .filter(nf -> !StringUtils.isNullOrEmpty(nf.getName()))
                    .filter(nf -> !StringUtils.isNullOrEmpty(nf.getValue()))
                    .toList();
            if (validFields.isEmpty()) {
                infoTA.setText("Please add one or more fields. Name and Value are both required.");
                return;
            }

            Document doc = new Document();
            try {
                for (NewField nf : validFields) {
                    doc.add(toIndexableField(nf));
                }
            } catch (NumberFormatException ex) {
                //FIXME: not using the ActionEvent for now, as DialogWrapper doesn't provide a doOKAction(AwtEvent) overload
                log.log(Level.SEVERE, "Error converting field value");
                throw new LukeException("Invalid value: " + ex.getMessage(), ex);
            } catch (Exception ex) {
                //FIXME: not using the ActionEvent for now, as DialogWrapper doesn't provide a doOKAction(AwtEvent) overload
                log.log(Level.SEVERE, "Error converting field value");
                throw new LukeException(ex.getMessage(), ex);
            }

            addDocument(doc);
            log.info("Added document: " + doc);
        }

        private IndexableField toIndexableField(NewField nf) throws Exception {
            final Constructor<? extends IndexableField> constr;
            if (nf.getType().equals(TextField.class) || nf.getType().equals(StringField.class)) {
                Field.Store store = nf.isStored() ? Field.Store.YES : Field.Store.NO;
                constr = nf.getType().getConstructor(String.class, String.class, Field.Store.class);
                return constr.newInstance(nf.getName(), nf.getValue(), store);
            } else if (nf.getType().equals(IntPoint.class)) {
                constr = nf.getType().getConstructor(String.class, int[].class);
                int[] values = NumericUtils.convertToIntArray(nf.getValue(), false);
                return constr.newInstance(nf.getName(), values);
            } else if (nf.getType().equals(LongPoint.class)) {
                constr = nf.getType().getConstructor(String.class, long[].class);
                long[] values = NumericUtils.convertToLongArray(nf.getValue(), false);
                return constr.newInstance(nf.getName(), values);
            } else if (nf.getType().equals(FloatPoint.class)) {
                constr = nf.getType().getConstructor(String.class, float[].class);
                float[] values = NumericUtils.convertToFloatArray(nf.getValue(), false);
                return constr.newInstance(nf.getName(), values);
            } else if (nf.getType().equals(DoublePoint.class)) {
                constr = nf.getType().getConstructor(String.class, double[].class);
                double[] values = NumericUtils.convertToDoubleArray(nf.getValue(), false);
                return constr.newInstance(nf.getName(), values);
            } else if (nf.getType().equals(SortedDocValuesField.class)
                       || nf.getType().equals(SortedSetDocValuesField.class)) {
                constr = nf.getType().getConstructor(String.class, BytesRef.class);
                return constr.newInstance(nf.getName(), new BytesRef(nf.getValue()));
            } else if (nf.getType().equals(NumericDocValuesField.class)
                       || nf.getType().equals(SortedNumericDocValuesField.class)) {
                constr = nf.getType().getConstructor(String.class, long.class);
                long value = NumericUtils.tryConvertToLongValue(nf.getValue());
                return constr.newInstance(nf.getName(), value);
            } else if (nf.getType().equals(StoredField.class)) {
                constr = nf.getType().getConstructor(String.class, String.class);
                return constr.newInstance(nf.getName(), nf.getValue());
            } else if (nf.getType().equals(Field.class)) {
                constr = nf.getType().getConstructor(String.class, String.class, IndexableFieldType.class);
                return constr.newInstance(nf.getName(), nf.getValue(), nf.getFieldType());
            } else {
                // TODO: unknown field
                return new StringField(nf.getName(), nf.getValue(), Field.Store.YES);
            }
        }

        private void addDocument(Document doc) {
            try {
                Analyzer analyzer =
                    operatorRegistry
                        .get(AnalysisTabOperator.class)
                        .map(AnalysisTabOperator::getCurrentAnalyzer)
                        .orElseGet(StandardAnalyzer::new);
                toolsModel.addDocument(doc, analyzer);
                indexHandler.reOpen();
                operatorRegistry
                    .get(DocumentsTabOperator.class)
                    .ifPresent(DocumentsTabOperator::displayLatestDoc);
                tabSwitcher.switchTab(TabbedPaneProvider.Tab.DOCUMENTS);
                infoTA.setText(MessageUtils.getLocalizedMessage("add_document.message.success"));
                setOKActionEnabled(false);
                setCancelButtonText(MessageUtils.getLocalizedMessage("button.close"));
            } catch (LukeException e) {
                infoTA.setText(MessageUtils.getLocalizedMessage("add_document.message.fail"));
                throw e;
            } catch (Exception e) {
                infoTA.setText(MessageUtils.getLocalizedMessage("add_document.message.fail"));
                throw new LukeException(e.getMessage(), e);
            }
        }
    }

    static final class FieldsTableModel extends TableModelBase<FieldsTableModel.Column> {

        enum Column implements TableColumnInfo {
            DEL("Del", 0, Boolean.class),
            NAME("Name", 1, String.class),
            TYPE("Type", 2, Class.class),
            OPTIONS("Options", 3, String.class),
            VALUE("Value", 4, String.class);

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

        private final List<NewField> newFieldList;

        FieldsTableModel(List<NewField> newFieldList) {
            super(newFieldList.size());
            this.newFieldList = newFieldList;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == Column.OPTIONS.getIndex()) {
                return "";
            }
            return data[rowIndex][columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != Column.OPTIONS.getIndex();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            data[rowIndex][columnIndex] = value;
            fireTableCellUpdated(rowIndex, columnIndex);
            NewField selectedField = newFieldList.get(rowIndex);
            if (columnIndex == Column.DEL.getIndex()) {
                selectedField.setDeleted((Boolean) value);
            } else if (columnIndex == Column.NAME.getIndex()) {
                selectedField.setName((String) value);
            } else if (columnIndex == Column.TYPE.getIndex()) {
                selectedField.setType((Class<? extends IndexableField>) value);
                selectedField.resetFieldType((Class<? extends IndexableField>) value);
                selectedField.setStored(selectedField.getFieldType().stored());
            } else if (columnIndex == Column.VALUE.getIndex()) {
                selectedField.setValue((String) value);
            }
        }

        @Override
        protected Column[] columnInfos() {
            return Column.values();
        }
    }

    static final class TypeCellRenderer implements TableCellRenderer {

        @SuppressWarnings("unchecked")
        @Override
        public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String simpleName = ((Class<? extends IndexableField>) value).getSimpleName();
            return new JLabel(simpleName);
        }
    }

    static final class OptionsCellRenderer implements TableCellRenderer {

        private final Project project;

        private JComponent parent;

        private final List<NewField> newFieldList;

        private final JPanel panel = new JPanel();

        private JTable table;

        public OptionsCellRenderer(
            Project project,
            JComponent parent,
            List<NewField> newFieldList) {
            this.project = project;
            this.parent = parent;
            this.newFieldList = newFieldList;
        }

        @Override
        public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (table != null && this.table != table) {
                this.table = table;
                final JTableHeader header = table.getTableHeader();
                if (header != null) {
                    panel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
                    panel.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
                    panel.add(new JLabel(value.toString()));

                    JLabel optionsLbl = new JLabel("options");
                    table.addMouseListener(
                        new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                int row = table.rowAtPoint(e.getPoint());
                                int col = table.columnAtPoint(e.getPoint());
                                if (row >= 0 && col == FieldsTableModel.Column.OPTIONS.getIndex()) {
                                    new IndexOptionsDialogFactory(project, parent, newFieldList.get(row)).show();
                                }
                            }
                        });
                    panel.add(FontUtils.toLinkText(optionsLbl));
                }
            }
            return panel;
        }
    }
}
