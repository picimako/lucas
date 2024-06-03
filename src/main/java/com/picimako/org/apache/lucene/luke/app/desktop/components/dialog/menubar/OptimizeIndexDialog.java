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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.JBScrollPane;
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.TextAreaPrintStream;
import org.apache.lucene.luke.models.tools.IndexTools;
import org.apache.lucene.util.NamedThreadFactory;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LICENSE NOTE: This class is extracted from {@link org.apache.lucene.luke.app.desktop.components.dialog.menubar.OptimizeIndexDialogFactory}.
 */
public class OptimizeIndexDialog extends DialogWrapper {

    private final IndexHandler indexHandler;

    private final JCheckBox expungeCB = new JCheckBox();

    private final JSpinner maxSegSpnr;

    private final JLabel statusLbl = new JLabel();

    private final JLabel indicatorLbl = new JLabel();

    private final JTextArea logArea = new JTextArea();

    private final ListenerFunctions listeners = new ListenerFunctions();

    private IndexTools toolsModel;

    public OptimizeIndexDialog(@Nullable Project project, IndexTools toolsModel) {
        super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);
        this.toolsModel = toolsModel;
        this.indexHandler = IndexHandler.getInstance();

        setTitle("Optimize index");
        setSize(900, 600); //increased width for better display of optimization log
        setOKButtonText(MessageUtils.getLocalizedMessage("optimize.button.optimize"));
        setOKButtonIcon(AllIcons.Actions.Profile);
        setCancelButtonText(MessageUtils.getLocalizedMessage("button.close"));

        initialize();
        maxSegSpnr = new JBIntSpinner(1, 1, 100, 1);
        maxSegSpnr.setPreferredSize(new Dimension(100, 30));

        init();
    }

    @Override
    protected void doOKAction() {
        listeners.optimize();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return content();
    }

    private void initialize() {
        expungeCB.setText(MessageUtils.getLocalizedMessage("optimize.checkbox.expunge"));
        expungeCB.setOpaque(false);

        indicatorLbl.setIcon(AnimatedIcon.Big.INSTANCE);

        logArea.setEditable(false);
        logArea.setLineWrap(true); //Enabled line wrap, so the horizontal scrollbar is not displayed
    }

    private JPanel content() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel.add(controller());
        panel.add(new JSeparator(JSeparator.HORIZONTAL));
        panel.add(logs());

        return panel;
    }

    private JPanel controller() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.setOpaque(false);

        JPanel idxPath = new JPanel(new FlowLayout(FlowLayout.LEADING));
        idxPath.setOpaque(false);
        idxPath.add(new JLabel(MessageUtils.getLocalizedMessage("optimize.label.index_path")));
        JLabel idxPathLbl = new JLabel(indexHandler.getState().getIndexPath());
        idxPathLbl.setToolTipText(indexHandler.getState().getIndexPath());
        idxPath.add(idxPathLbl);
        panel.add(idxPath);

        JPanel expunge = new JPanel(new FlowLayout(FlowLayout.LEADING));
        expunge.setOpaque(false);

        expunge.add(expungeCB);
        panel.add(expunge);

        JPanel maxSegs = new JPanel(new FlowLayout(FlowLayout.LEADING));
        maxSegs.setOpaque(false);
        maxSegs.add(new JLabel(MessageUtils.getLocalizedMessage("optimize.label.max_segments")));
        maxSegs.add(maxSegSpnr);
        panel.add(maxSegs);

        return panel;
    }

    private JPanel logs() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        header.add(new JLabel(MessageUtils.getLocalizedMessage("optimize.label.note")));
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEADING));
        status.setOpaque(false);
        status.add(new JLabel(MessageUtils.getLocalizedMessage("label.status")));
        statusLbl.setText("Idle");
        status.add(statusLbl);
        indicatorLbl.setVisible(false);
        status.add(indicatorLbl);
        header.add(status);
        panel.add(header, BorderLayout.PAGE_START);

        logArea.setText("");
        panel.add(new JBScrollPane(logArea), BorderLayout.CENTER);

        return panel;
    }

    private class ListenerFunctions {

        void optimize() {
            ExecutorService executor =
                Executors.newFixedThreadPool(1, new NamedThreadFactory("optimize-index-dialog"));

            SwingWorker<Void, Void> task =
                new SwingWorker<Void, Void>() {

                    @Override
                    protected Void doInBackground() {
                        setProgress(0);
                        statusLbl.setText("Running...");
                        indicatorLbl.setVisible(true);
                        try (var ps = new TextAreaPrintStream(logArea)) {
                            toolsModel.optimize(expungeCB.isSelected(), (int) maxSegSpnr.getValue(), ps);
                            ps.flush();
                        } catch (Exception e) {
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
                        statusLbl.setText("Done");
                        indexHandler.reOpen();
                    }
                };

            executor.submit(task);
            executor.shutdown();
        }
    }
}
