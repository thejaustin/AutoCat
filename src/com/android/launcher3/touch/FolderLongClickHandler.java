/*
 * Copyright (C) 2024 The AutoCat Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.touch;

import android.view.View;

import com.android.launcher3.model.data.FolderInfo;

/**
 * Implemented by launchers that want to intercept long-press on workspace folder icons
 * to show a context menu (Archive All / Restore All / Move) instead of immediately
 * starting a drag operation.
 */
public interface FolderLongClickHandler {
    /**
     * Called when the user long-presses a folder icon on the workspace.
     *
     * @param folderInfo the folder that was long-pressed
     * @param view       the folder icon view
     */
    void onFolderLongClick(FolderInfo folderInfo, View view);
}
