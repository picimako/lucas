//Copyright 2024 Tamás Balog. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.picimako.lucas;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.RunOnceUtil;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Performs actions on project startup.
 */
public final class ProjectStartupActivity implements ProjectActivity {

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        RunOnceUtil.runOnceForApp("Lucas v0.4.0 - JDK 21", () -> {
            var notification = NotificationGroupManager.getInstance().getNotificationGroup("Lucas JDK 21 Configuration")
                .createNotification("Lucas v0.4.0 is built on JDK 21, and it requires manual configuration which is detailed in the documentation.", NotificationType.WARNING)
                .setSubtitle("Lucas JDK 21 Configuration")
                .addAction(new AnAction("Open Documentation") {

                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        BrowserUtil.browse("https://github.com/picimako/lucas/blob/main/README.md#intellij-20242-and-jdk-21-support");
                    }
                });

            Notifications.Bus.notify(notification, project);
        });

        return Unit.INSTANCE;
    }
}
