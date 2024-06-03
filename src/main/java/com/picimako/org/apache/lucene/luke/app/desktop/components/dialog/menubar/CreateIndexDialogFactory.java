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
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.PathUtil;
import com.picimako.org.apache.lucene.luke.app.IndexHandler;
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain;
import com.picimako.org.apache.lucene.luke.app.desktop.PreferencesImpl;
import org.apache.lucene.luke.app.desktop.util.FontUtils;
import org.apache.lucene.luke.app.desktop.util.MessageUtils;
import org.apache.lucene.luke.app.desktop.util.URLLabel;
import org.apache.lucene.luke.models.tools.IndexTools;
import org.apache.lucene.luke.models.tools.IndexToolsFactory;
import org.apache.lucene.luke.util.LoggerFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.NamedThreadFactory;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory of create index dialog
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.dialog.menubar.CreateIndexDialogFactory}.
 */
public class CreateIndexDialogFactory extends DialogWrapper {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final IndexHandler indexHandler;

  private final JTextField locationTF = new JTextField();

  private final JButton browseBtn = new JButton();

  private final JTextField dirnameTF = new JTextField();

  private final JTextField dataDirTF = new JTextField();

  private final JButton dataBrowseBtn = new JButton();

  private final JButton clearBtn = new JButton();

  private final JLabel indicatorLbl = new JLabel();

  private final ListenerFunctions listeners;

  public CreateIndexDialogFactory(@Nullable Project project) throws IOException {
    super(project, LukeMain.getOwnerFrame(), false, IdeModalityType.IDE);
    this.indexHandler = IndexHandler.getInstance();
    this.listeners = new ListenerFunctions(project);

    setTitle(MessageUtils.getLocalizedMessage("createindex.dialog.title"));
    setSize(600, 360);
    initialize();
    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return content();
  }

  private void initialize() {
    locationTF.setPreferredSize(new Dimension(360, 30));
    locationTF.setText(System.getProperty("user.home"));
    locationTF.setEditable(false);

    browseBtn.setText(MessageUtils.getLocalizedMessage("button.browse"));
    browseBtn.setIcon(AllIcons.Actions.MenuOpen);
    browseBtn.setPreferredSize(new Dimension(120, 30));
    browseBtn.addActionListener(listeners::browseLocationDirectory);

    dirnameTF.setPreferredSize(new Dimension(200, 30));

    dataDirTF.setPreferredSize(new Dimension(250, 30));
    dataDirTF.setEditable(false);

    clearBtn.setText(MessageUtils.getLocalizedMessage("button.clear"));
    clearBtn.setPreferredSize(new Dimension(70, 30));
    clearBtn.addActionListener(listeners::clearDataDir);

    dataBrowseBtn.setText(MessageUtils.getLocalizedMessage("button.browse"));
    dataBrowseBtn.setIcon(AllIcons.Actions.MenuOpen);
    dataBrowseBtn.setPreferredSize(new Dimension(100, 30));
    dataBrowseBtn.addActionListener(listeners::browseDataDirectory);

    indicatorLbl.setIcon(AnimatedIcon.Big.INSTANCE);
    indicatorLbl.setVisible(false);

    setOKButtonText(MessageUtils.getLocalizedMessage("button.create"));
  }

  @Override
  protected void doOKAction() {
    listeners.createIndex();
  }

  private JPanel content() {
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    panel.add(basicSettings());
    panel.add(new TitledSeparator(MessageUtils.getLocalizedMessage("createindex.label.option")));
    panel.add(optionalSettings());
    panel.add(new TitledSeparator());
    panel.add(buttons());

    return panel;
  }

  private JPanel basicSettings() {
    JPanel panel = new JPanel(new GridLayout(2, 1));
    panel.setOpaque(false);

    JPanel locPath = new JPanel(new FlowLayout(FlowLayout.LEADING));
    locPath.setOpaque(false);
    locPath.add(new JLabel(MessageUtils.getLocalizedMessage("createindex.label.location")));
    locPath.add(locationTF);
    locPath.add(browseBtn);
    panel.add(locPath);

    JPanel dirName = new JPanel(new FlowLayout(FlowLayout.LEADING));
    dirName.setOpaque(false);
    dirName.add(new JLabel(MessageUtils.getLocalizedMessage("createindex.label.dirname")));
    dirName.add(dirnameTF);
    panel.add(dirName);

    return panel;
  }

  private JPanel optionalSettings() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);

    JPanel description = new JPanel();
    description.setLayout(new BoxLayout(description, BoxLayout.Y_AXIS));
    description.setOpaque(false);

    JTextArea descTA1 =
        new JTextArea(MessageUtils.getLocalizedMessage("createindex.textarea.data_help1"));
    descTA1.setPreferredSize(new Dimension(550, 20));
    descTA1.setBorder(BorderFactory.createEmptyBorder(2, 10, 10, 5));
    descTA1.setOpaque(false);
    descTA1.setEditable(false);
    description.add(descTA1);

    JPanel link = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 1));
    link.setOpaque(false);
    JLabel linkLbl =
        FontUtils.toLinkText(
            new URLLabel(MessageUtils.getLocalizedMessage("createindex.label.data_link")));
    link.add(linkLbl);
    description.add(link);

    JTextArea descTA2 =
        new JTextArea(MessageUtils.getLocalizedMessage("createindex.textarea.data_help2"));
    descTA2.setPreferredSize(new Dimension(550, 50));
    descTA2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
    descTA2.setOpaque(false);
    descTA2.setEditable(false);
    description.add(descTA2);

    panel.add(description, BorderLayout.PAGE_START);

    JPanel dataDirPath = new JPanel(new FlowLayout(FlowLayout.LEADING));
    dataDirPath.setOpaque(false);
    dataDirPath.add(new JLabel(MessageUtils.getLocalizedMessage("createindex.label.datadir")));
    dataDirPath.add(dataDirTF);
    dataDirPath.add(dataBrowseBtn);

    dataDirPath.add(clearBtn);
    panel.add(dataDirPath, BorderLayout.CENTER);

    return panel;
  }

  private JPanel buttons() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 10, 20));

    panel.add(indicatorLbl);

    return panel;
  }

  private class ListenerFunctions {
    private final Project project;

    public ListenerFunctions(Project project) {
      this.project = project;
    }

    void browseLocationDirectory(ActionEvent e) {
      browseDirectory(locationTF);
    }

    void browseDataDirectory(ActionEvent e) {
      browseDirectory(dataDirTF);
    }

    private void browseDirectory(JTextField tf) {
      FileChooser.chooseFile(
          //Selects directories only
          new FileChooserDescriptor(false, true, false, false, false, false)
              .withShowHiddenFiles(true),
          project,
          LukeMain.getOwnerFrame(),
          VfsUtil.findFileByIoFile(new File(tf.getText()), true),
          selectedDirectory -> tf.setText(PathUtil.toSystemDependentName(selectedDirectory.getPath())));
    }

    void createIndex() {
      Path path = Paths.get(locationTF.getText(), dirnameTF.getText());
      if (Files.exists(path)) {
        String message = "The directory " + path.toAbsolutePath().toString() + " already exists.";
        JOptionPane.showMessageDialog(
            getContentPanel(), message, "Empty index path", JOptionPane.ERROR_MESSAGE);
      } else {
        // create new index asynchronously
        ExecutorService executor =
            Executors.newFixedThreadPool(1, new NamedThreadFactory("create-index-dialog"));

        SwingWorker<Void, Void> task =
            new SwingWorker<Void, Void>() {
              /**
               * This is to indicate whether the index creation was successful, and close the dialog only when it was.
               * <p>
               * This approach is used instead of retrieving the result of {@code doInBackground()} in {@code done()},
               * because currently there is only one return location in {@code doInBackground()}, and I'd like to keep
               * the implementation as close to the original as possible.
               */
              private boolean isCreationSuccessful;

              @Override
              protected Void doInBackground() throws Exception {
                isCreationSuccessful = true;
                setProgress(0);
                indicatorLbl.setVisible(true);
                setOKActionEnabled(false);

                try {
                  Directory dir = FSDirectory.open(path);
                  IndexTools toolsModel = new IndexToolsFactory().newInstance(dir);

                  if (dataDirTF.getText().isEmpty()) {
                    // without sample documents
                    toolsModel.createNewIndex();
                  } else {
                    // with sample documents
                    Path dataPath = Paths.get(dataDirTF.getText());
                    toolsModel.createNewIndex(dataPath.toAbsolutePath().toString());
                  }

                  indexHandler.open(path.toAbsolutePath().toString(), null, false, false, false);
                  PreferencesImpl.getInstance().addHistory(path.toAbsolutePath().toString());

                  dirnameTF.setText("");
                } catch (Exception ex) {
                  isCreationSuccessful = false;
                  // cleanup
                  try {
                    Files.walkFileTree(
                        path,
                        new SimpleFileVisitor<Path>() {
                          @Override
                          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                              throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                          }
                        });
                    Files.deleteIfExists(path);
                  } catch (
                      @SuppressWarnings("unused")
                      IOException ex2) {
                  }

                  log.log(Level.SEVERE, "Cannot create index", ex);
                  String message = "See Logs tab for more details.";
                  JOptionPane.showMessageDialog(
                      getContentPanel(), message, "Cannot create index", JOptionPane.ERROR_MESSAGE);
                } finally {
                  setProgress(100);
                }
                return null;
              }

              @Override
              protected void done() {
                indicatorLbl.setVisible(false);
                setOKActionEnabled(true);

                /*
                 * Closes the dialog only when the index creation was successful.
                 *
                 * This close operation is moved here from 'doInBackground()' because closing a DialogWrapper
                 * must happen on the EDT.
                 *
                 * If the dialog is closed during the index creation, it is not a problem. DialogWrapper's underlying
                 * code handles that case, and doesn't try to close it.
                 */
                if (isCreationSuccessful)
                  doCancelAction();
              }
            };

        executor.submit(task);
        executor.shutdown();
      }
    }

    private void clearDataDir(ActionEvent e) {
      dataDirTF.setText("");
    }
  }
}
