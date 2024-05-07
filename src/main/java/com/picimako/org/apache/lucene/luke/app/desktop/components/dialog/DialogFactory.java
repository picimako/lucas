package com.picimako.org.apache.lucene.luke.app.desktop.components.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

/**
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.util.DialogOpener.DialogFactory}.
 */
public interface DialogFactory<T extends DialogWrapper> {

    T createDialog(@NotNull Project project);
}
