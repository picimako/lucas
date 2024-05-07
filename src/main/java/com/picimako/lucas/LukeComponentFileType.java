//Copyright 2024 Tamás Balog. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.picimako.lucas;

import com.intellij.openapi.fileEditor.impl.JComponentFileType;

/**
 * Custom file type for the Luke tab.
 */
public final class LukeComponentFileType extends JComponentFileType {
    public static final LukeComponentFileType INSTANCE = new LukeComponentFileType();
}
