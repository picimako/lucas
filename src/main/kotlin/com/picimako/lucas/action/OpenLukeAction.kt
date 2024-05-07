//Copyright 2024 Tamás Balog. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.picimako.lucas.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.impl.JComponentEditorProviderUtils.openEditor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.picimako.lucas.LucasBundle
import com.picimako.lucas.LukeComponentFileType
import com.picimako.org.apache.lucene.luke.app.DirectoryHandler
import com.picimako.org.apache.lucene.luke.app.IndexHandler
import com.picimako.org.apache.lucene.luke.app.desktop.LukeMain
import com.picimako.org.apache.lucene.luke.app.desktop.MessageBroker
import com.picimako.org.apache.lucene.luke.app.desktop.components.ComponentOperatorRegistry
import com.picimako.org.apache.lucene.luke.app.desktop.components.TabSwitcherProxy

/**
 * The entry point for opening the ported Luke - Lucene Toolbox.
 *
 * It opens a Luke in a new editor tab. Currently only one Luke instance is allowed to be open,
 * so if a Luke tab is already open, this action focuses on it, instead of opening a new one.
 */
class OpenLukeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        //Always rebuild the Luke UI from scratch
        val createdGUI = LukeMain.createGUI(e.project)

        if (!createdGUI) {
            Messages.showErrorDialog("Could not initialize Luke.", "Error Loading Luke")
            return
        }

        //If the Luke tab is already open, we don't open a new one, instead, we simply focus on the already open tab.
        val fileEditorManager = FileEditorManager.getInstance(e.project!!)
        fileEditorManager.allEditors
            .asSequence()
            .find { LucasBundle.message("action.open.luke.text") == it.name }
            ?.let {
                fileEditorManager.openFile(it.file, true, true)
                return
            }

        //If not already open, we create a new editor and open it
        val fileEditors = openEditor(
            e.project!!,
            LucasBundle.message("action.open.luke.text"),
            LukeMain.getOwnerFrame(),
            LukeComponentFileType.INSTANCE
        )

        //If there is only one editor created, and there hasn't been a FileEditorManagerListener registered,
        // we register one, so that various data storage light service classes can be cleared upon closing the Luke tab.
        if (fileEditors.size == 1 && !isEditorCloseListenerRegistered) {
            e.project!!.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    if (LucasBundle.message("action.open.luke.text") == file.name) {
                        //Dispose 'frame'
                        LukeMain.clear()

                        //Clear helper storages in order to be able to re-initialize the Luke editor tab from scratch upon reopen.
                        DirectoryHandler.getInstance().close()
                        DirectoryHandler.getInstance().clear()

                        IndexHandler.getInstance().close()
                        IndexHandler.getInstance().clear()

                        ComponentOperatorRegistry.getInstance().clear()

                        MessageBroker.getInstance().clear()

                        TabSwitcherProxy.getInstance().clear()
                    }

                    super.fileClosed(source, file)
                }
            })
            isEditorCloseListenerRegistered = true
        }
    }

    companion object {
        /**
         * This is so, that the FileEditorManagerListener is registered only once for the whole application.
         */
        private var isEditorCloseListenerRegistered = false
    }
}