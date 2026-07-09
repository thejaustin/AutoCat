/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.launcher3.folder

import android.content.Context
import androidx.core.util.Consumer
import com.android.launcher3.LauncherModel
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.util.Executors.MODEL_EXECUTOR
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class FolderNameSuggestionLoader
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val folderNameProviderFactory: Provider<FolderNameProvider>,
    private val model: LauncherModel,
) {

    var folderNameProvider: FolderNameProvider? = null

    init {
        MODEL_EXECUTOR.execute {
            folderNameProvider = folderNameProviderFactory.get()
            model.enqueueModelUpdateTask { _, dataModel, appList ->
                folderNameProvider?.load(
                    appList.copyData().asList(),
                    FolderNameProvider.getCollectionForSuggestions(dataModel),
                )
            }
        }
    }

    fun getSuggestedFolderName(
        workspaceItemInfos: ArrayList<WorkspaceItemInfo>,
        callback: Consumer<FolderNameInfos>,
    ) {
        MODEL_EXECUTOR.execute {
            val nameInfos = FolderNameInfos()
            folderNameProvider?.getSuggestedFolderName(context, workspaceItemInfos, nameInfos)

            val prefs = app.lawnchair.preferences.PreferenceManager.getInstance(context)
            if (prefs.autoCatGenAIFolderNaming.get()) {
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val appNames = workspaceItemInfos.mapNotNull { it.title?.toString() }.filter { it.isNotBlank() }
                        if (appNames.size >= 2) {
                            val providerId = prefs.llmProviderPreference.get()
                            val provider = when (providerId) {
                                "google_ai" -> app.lawnchair.categorization.llm.GoogleAIProvider(context)
                                "claude" -> app.lawnchair.categorization.llm.ClaudeProvider(context)
                                "openai" -> app.lawnchair.categorization.llm.OpenAIProvider(context)
                                "perplexity" -> app.lawnchair.categorization.llm.PerplexityProvider(context)
                                else -> app.lawnchair.categorization.llm.GoogleAIProvider(context)
                            }
                            
                            val prompt = "I am creating a folder with the following apps: ${appNames.joinToString(", ")}. What is a concise, 1-2 word name for this folder? Return ONLY the folder name, nothing else."
                            val suggestedName = provider.generateText(prompt)
                            if (suggestedName.isNotBlank() && suggestedName.length < 25) {
                                val cleanName = suggestedName.replace("\"", "").trim()
                                val newNameInfos = FolderNameInfos()
                                newNameInfos.setStatus(FolderNameInfos.HAS_PRIMARY)
                                val baseLabels = nameInfos.labels?.filterNotNull() ?: emptyList()
                                val newLabels = (listOf(cleanName) + baseLabels).distinct().toTypedArray()
                                for ((index, label) in newLabels.withIndex()) {
                                    newNameInfos.setLabel(index, label, 1.0f - (index * 0.1f))
                                }
                                com.android.launcher3.util.Executors.MAIN_EXECUTOR.execute {
                                    callback.accept(newNameInfos)
                                }
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FolderNameSuggest", "LLM naming failed", e)
                    }
                    com.android.launcher3.util.Executors.MAIN_EXECUTOR.execute {
                        callback.accept(nameInfos)
                    }
                }
            } else {
                com.android.launcher3.util.Executors.MAIN_EXECUTOR.execute {
                    callback.accept(nameInfos)
                }
            }
        }
    }
}
