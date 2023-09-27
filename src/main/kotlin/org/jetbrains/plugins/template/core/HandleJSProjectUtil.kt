package org.jetbrains.plugins.template.core

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.find.FindSettings
import com.intellij.find.actions.ShowUsagesAction
import com.intellij.find.impl.FindInProjectUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.UsageViewPresentation
import java.util.concurrent.atomic.AtomicInteger

class HandleJSProjectUtil {

    companion object {
        fun hand(filePath : String, fullPath : String, project : Project) : Boolean {
            //搜索结果
            val usageList = mutableListOf<Usage>()
            val findModel = FindManager.getInstance(project).findInProjectModel
            //搜索路径
            findModel.directoryName = filePath
            findModel.isGlobal = false
            findModel.isReplaceState = false
            findModel.isProjectScope = false
            findModel.moduleName = null
            findModel.isWithSubdirectories = true
            findModel.isMultipleFiles = true
            // 设置了排除注释，但仍然没排除，应该没识别出注释，因为如果设置仅注释就查不出来
            // 考虑stringToFind正则表达式
            findModel.searchContext = FindModel.SearchContext.EXCEPT_COMMENTS
            findModel.fileFilter = "*.vue,*.js"
            //搜索内容
            findModel.stringToFind = fullPath
            val findSettings = FindSettings.getInstance()
            //搜索范围
            findSettings.defaultScopeName = "Directory"
            val myUsageViewPresentation = UsageViewPresentation()
            val processPresentation = FindInProjectUtil.setupProcessPresentation(project!!, myUsageViewPresentation!!)
            val filesToScanInitially: Set<VirtualFile?> = LinkedHashSet()
            val resultsCount = AtomicInteger()
            val state = ModalityState.current()
            FindInProjectUtil.findUsages(findModel, project!!, processPresentation, filesToScanInitially) { info ->
                val usage = UsageInfo2UsageAdapter.CONVERTER.`fun`(info)
                usageList.add(usage)
                val continueSearch = resultsCount.incrementAndGet() < ShowUsagesAction.getUsagesPageSize()
                return@findUsages continueSearch
            }
            if (usageList.size > 0) {
                return true
            }
            return false
        }
    }

}