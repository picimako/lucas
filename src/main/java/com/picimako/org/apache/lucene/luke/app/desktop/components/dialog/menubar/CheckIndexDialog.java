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
import com.intellij.ui.components.JBScrollPane;
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.luke.app.LukeState;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.TextAreaPrintStream;
import org.apache.lucene.luke.models.tools.IndexTools;
import org.apache.lucene.luke.util.LoggerFactory;
import org.apache.lucene.util.NamedThreadFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LICENSE NOTE: This class is extracted from {@link org.apache.lucene.luke.app.desktop.components.dialog.menubar.CheckIndexDialogFactory}.
 */
public class CheckIndexDialog extends DialogWrapper {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IndexHandler indexHandler;

    private LukeState lukeState;

    private CheckIndex.Status status;

    private IndexTools toolsModel;

    private final JLabel resultLbl = new JLabel();

    private final JLabel statusLbl = new JLabel();

    private final JLabel indicatorLbl = new JLabel();

    private final JButton repairBtn = new JButton();

    private final JTextArea logArea = new JTextArea();

    private final ListenerFunctions listeners = new ListenerFunctions();

    public CheckIndexDialog(@NotNull Project project, IndexTools toolsModel, LukeState lukeState) {
        super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);
        this.toolsModel = toolsModel;
        this.lukeState = lukeState;
        this.indexHandler = IndexHandler.getInstance();

        setTitle("Check index");
        setSize(800, 600); //increased width to allow more logs to be visible
        setOKButtonText(MessageUtils.getLocalizedMessage("checkidx.button.check"));
        setOKButtonIcon(AllIcons.Actions.Find);
        setCancelButtonText(MessageUtils.getLocalizedMessage("button.close"));

        initialize();
        init();
    }

    @Override
    protected void doOKAction() {
        listeners.checkIndex();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return content();
    }

    private void initialize() {
        repairBtn.setText(MessageUtils.getLocalizedMessage("checkidx.button.fix"));
        repairBtn.setIcon(AllIcons.Toolwindows.ToolWindowBuild);
        repairBtn.setEnabled(false);
        repairBtn.addActionListener(listeners::repairIndex);

        indicatorLbl.setIcon(AnimatedIcon.Big.INSTANCE);

        logArea.setEditable(false);
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
        idxPath.add(new JLabel(MessageUtils.getLocalizedMessage("checkidx.label.index_path")));
        JLabel idxPathLbl = new JLabel(lukeState.getIndexPath());
        idxPathLbl.setToolTipText(lukeState.getIndexPath());
        idxPath.add(idxPathLbl);
        panel.add(idxPath);

        JPanel results = new JPanel(new GridLayout(2, 1));
        results.setOpaque(false);
        results.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        results.add(new JLabel(MessageUtils.getLocalizedMessage("checkidx.label.results")));
        results.add(resultLbl);
        panel.add(results);

        return panel;
    }

    private JPanel logs() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.PAGE_AXIS));

        JPanel repair = new JPanel(new FlowLayout(FlowLayout.LEADING));
        repair.setOpaque(false);
        repair.add(repairBtn);

        JTextArea warnArea =
            new JTextArea(MessageUtils.getLocalizedMessage("checkidx.label.warn"), 3, 30);
//        warnArea.setLineWrap(true);
        warnArea.setEditable(false);
        warnArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        repair.add(warnArea);
        header.add(repair);

        JPanel note = new JPanel(new FlowLayout(FlowLayout.LEADING));
        note.setOpaque(false);
        note.add(new JLabel(MessageUtils.getLocalizedMessage("checkidx.label.note")));
        header.add(note);

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

        void checkIndex() {
            ExecutorService executor =
                Executors.newFixedThreadPool(1, new NamedThreadFactory("check-index-dialog-check"));

            SwingWorker<CheckIndex.Status, Void> task =
                new SwingWorker<CheckIndex.Status, Void>() {

                    @Override
                    protected CheckIndex.Status doInBackground() {
                        setProgress(0);
                        statusLbl.setText("Running...");
                        indicatorLbl.setVisible(true);
                        TextAreaPrintStream ps;
                        try {
                            ps = new TextAreaPrintStream(logArea);
                            CheckIndex.Status status = toolsModel.checkIndex(ps);
                            ps.flush();
                            return status;
                        } catch (Exception e) {
                            statusLbl.setText(MessageUtils.getLocalizedMessage("message.error.unknown"));
                            throw e;
                        } finally {
                            setProgress(100);
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            CheckIndex.Status st = get();
                            resultLbl.setText(createResultsMessage(st));
                            indicatorLbl.setVisible(false);
                            statusLbl.setText("Done");
                            if (!st.clean) {
                                repairBtn.setEnabled(true);
                            }
                            status = st;
                        } catch (Exception e) {
                            log.log(Level.SEVERE, "Error checking index", e);
                            statusLbl.setText(MessageUtils.getLocalizedMessage("message.error.unknown"));
                        }
                    }
                };

            executor.submit(task);
            executor.shutdown();
        }

        private String createResultsMessage(CheckIndex.Status status) {
            String msg;
            if (status == null) {
                msg = "?";
            } else if (status.clean) {
                msg = "OK";
            } else if (status.toolOutOfDate) {
                msg = "ERROR: Can't check - tool out-of-date";
            } else {
                StringBuilder sb = new StringBuilder("BAD:");
                if (status.missingSegments) {
                    sb.append(" Missing segments.");
                }
                if (status.numBadSegments > 0) {
                    sb.append(" numBadSegments=");
                    sb.append(status.numBadSegments);
                }
                if (status.totLoseDocCount > 0) {
                    sb.append(" totLoseDocCount=");
                    sb.append(status.totLoseDocCount);
                }
                msg = sb.toString();
            }
            return msg;
        }

        void repairIndex(ActionEvent e) {
            if (status == null) {
                return;
            }

            ExecutorService executor =
                Executors.newFixedThreadPool(1, new NamedThreadFactory("check-index-dialog-repair"));

            SwingWorker<CheckIndex.Status, Void> task =
                new SwingWorker<CheckIndex.Status, Void>() {

                    @Override
                    protected CheckIndex.Status doInBackground() {
                        setProgress(0);
                        statusLbl.setText("Running...");
                        indicatorLbl.setVisible(true);
                        logArea.setText("");
                        TextAreaPrintStream ps;
                        try {
                            ps = new TextAreaPrintStream(logArea);
                            toolsModel.repairIndex(status, ps);
                            statusLbl.setText("Done");
                            ps.flush();
                            return status;
                        } catch (Exception e) {
                            statusLbl.setText(MessageUtils.getLocalizedMessage("message.error.unknown"));
                            throw e;
                        } finally {
                            setProgress(100);
                        }
                    }

                    @Override
                    protected void done() {
                        indexHandler.open(lukeState.getIndexPath(), lukeState.getDirImpl());
                        logArea.append("Repairing index done.");
                        resultLbl.setText("");
                        indicatorLbl.setVisible(false);
                        repairBtn.setEnabled(false);
                    }
                };

            executor.submit(task);
            executor.shutdown();
        }
    }
}
