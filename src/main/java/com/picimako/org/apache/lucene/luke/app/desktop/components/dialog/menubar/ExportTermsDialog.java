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

package com.picimako.org.apache.lucene.luke.app.desktop.components.dialog.menubar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import org.apache.lucene.luke.app.LukeState;
import org.apache.lucene.luke.app.desktop.util.ImageUtils;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.StyleConstants;
import org.apache.lucene.luke.models.LukeException;
import org.apache.lucene.luke.models.tools.IndexTools;
import org.apache.lucene.luke.models.util.IndexUtils;
import org.apache.lucene.luke.util.LoggerFactory;
import org.apache.lucene.util.NamedThreadFactory;
import org.apache.lucene.util.SuppressForbidden;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * LICENSE NOTE: This class is extracted from {@link org.apache.lucene.luke.app.desktop.components.dialog.menubar.ExportTermsDialogFactory}.
 */
public class ExportTermsDialog extends DialogWrapper {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final JComboBox<String> fieldCombo = new ComboBox<>();

    private final JComboBox<String> delimiterCombo = new ComboBox<>();

    private final JTextField destDir = new JTextField();

    private final JLabel statusLbl = new JLabel();

    private final JLabel indicatorLbl = new JLabel();

    private final ListenerFunctions listeners = new ListenerFunctions();

    private final IndexHandler indexHandler;

    private IndexTools toolsModel;

    private String selectedDelimiter;

    public ExportTermsDialog(@Nullable Project project, IndexTools toolsModel, LukeState lukeState) {
        super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);
        this.toolsModel = toolsModel;
        this.indexHandler = IndexHandler.getInstance();
        Stream.of(Delimiter.values())
            .forEachOrdered(delimiterVal -> delimiterCombo.addItem(delimiterVal.getDescription()));
        delimiterCombo.setSelectedItem(Delimiter.COMMA.getDescription()); // Set default delimiter

        IndexUtils.getFieldNames(lukeState.getIndexReader()).stream()
            .sorted()
            .forEach(fieldCombo::addItem);

        setTitle("Export terms");
        setSize(600, 450);
        setOKButtonText(MessageUtils.getLocalizedMessage("export.terms.button.export"));
        setCancelButtonText(MessageUtils.getLocalizedMessage("button.close"));

        init();
    }

    @Override
    protected void doOKAction() {
        listeners.export();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return content();
    }

    private JPanel content() {
        JPanel panel = new JPanel(new GridLayout(5, 1));
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel.add(currentOpenIndexPanel());
        panel.add(fieldComboPanel());
        panel.add(destinationDirPanel());
        panel.add(delimiterComboPanel());
        panel.add(statusPanel());

        return panel;
    }

    private JPanel currentOpenIndexPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        panel.setBorder(BorderFactory.createEmptyBorder());
        panel.setOpaque(false);
        JLabel label = new JLabel(MessageUtils.getLocalizedMessage("export.terms.label.index_path"));
        JLabel value = new JLabel(indexHandler.getState().getIndexPath());
        value.setToolTipText(indexHandler.getState().getIndexPath());
        panel.add(label);
        panel.add(value);
        return panel;
    }

    private JPanel delimiterComboPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setOpaque(false);
        panel.add(new JLabel("Select Delimiter: "));
        panel.add(delimiterCombo);
        return panel;
    }

    private JPanel fieldComboPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setOpaque(false);
        panel.add(new JLabel(MessageUtils.getLocalizedMessage("export.terms.field")));
        panel.add(fieldCombo);
        return panel;
    }

    private JPanel destinationDirPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setOpaque(false);

        panel.add(new JLabel(MessageUtils.getLocalizedMessage("export.terms.label.output_path")));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        inputPanel.setBorder(BorderFactory.createEmptyBorder());
        inputPanel.setOpaque(false);
        destDir.setText(System.getProperty("user.home"));
        destDir.setColumns(60);
        destDir.setPreferredSize(new Dimension(200, 30));
        destDir.setFont(StyleConstants.FONT_MONOSPACE_LARGE);
        destDir.setEditable(false);
        destDir.setBackground(JBColor.WHITE);
        inputPanel.add(destDir);

        JButton browseBtn = new JButton(MessageUtils.getLocalizedMessage("export.terms.button.browse"));
        browseBtn.addActionListener(listeners::browseDirectory);
        inputPanel.add(browseBtn);

        panel.add(inputPanel);
        return panel;
    }

    private JPanel statusPanel() {
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEADING));
        status.setOpaque(false);
        indicatorLbl.setIcon(ImageUtils.createImageIcon("indicator.gif", 20, 20));
        indicatorLbl.setVisible(false);
        status.add(statusLbl);
        status.add(indicatorLbl);
        return status;
    }

    private class ListenerFunctions {

        @SuppressForbidden(reason = "JFilechooser#getSelectedFile() returns java.io.File")
        void browseDirectory(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setFileHidingEnabled(false);
            int retVal = fileChooser.showOpenDialog(getContentPanel());
            if (retVal == JFileChooser.APPROVE_OPTION) {
                File f = fileChooser.getSelectedFile();
                destDir.setText(f.getAbsolutePath());
            }
        }

        void export() {
            ExecutorService executor =
                Executors.newSingleThreadExecutor(new NamedThreadFactory("export-terms-dialog"));

            SwingWorker<Void, Void> task =
                new SwingWorker<Void, Void>() {

                    String filename;

                    @Override
                    protected Void doInBackground() {
                        setProgress(0);
                        statusLbl.setText("Exporting...");
                        indicatorLbl.setVisible(true);
                        String field = (String) fieldCombo.getSelectedItem();
                        selectedDelimiter =
                            Delimiter.getSelectedDelimiterValue((String) delimiterCombo.getSelectedItem());

                        String directory = destDir.getText();
                        try {
                            filename = toolsModel.exportTerms(directory, field, selectedDelimiter);
                        } catch (LukeException e) {
                            log.log(Level.SEVERE, "Error while exporting terms from field " + field, e);
                            statusLbl.setText(
                                MessageUtils.getLocalizedMessage("export.terms.label.error", e.getMessage()));
                        } catch (Exception e) {
                            log.log(Level.SEVERE, "Error while exporting terms from field " + field, e);
                            statusLbl.setText(MessageUtils.getLocalizedMessage("message.error.unknown"));
                            throw e;
                        } finally {
                            setProgress(100);
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        indicatorLbl.setVisible(false);
                        if (filename != null) {
                            statusLbl.setText(
                                MessageUtils.getLocalizedMessage(
                                    "export.terms.label.success",
                                    filename,
                                    "[term]" + selectedDelimiter + "[doc frequency]"));
                        }
                    }
                };

            executor.submit(task);
            executor.shutdown();
        }
    }

    /** Delimiters that can be selected */
    private enum Delimiter {
        COMMA("Comma", ","),
        WHITESPACE("Whitespace", " "),
        TAB("Tab", "\t");

        private final String description;
        private final String separator;

        private Delimiter(final String description, final String separator) {
            this.description = description;
            this.separator = separator;
        }

        String getDescription() {
            return this.description;
        }

        String getSeparator() {
            return this.separator;
        }

        static String getSelectedDelimiterValue(String delimiter) {
            return Arrays.stream(Delimiter.values())
                .filter(e -> e.description.equals(delimiter))
                .findFirst()
                .orElse(COMMA)
                .getSeparator();
        }
    }
}
