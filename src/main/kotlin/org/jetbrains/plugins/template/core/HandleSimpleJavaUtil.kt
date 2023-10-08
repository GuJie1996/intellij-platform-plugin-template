package org.jetbrains.plugins.template.core

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.find.FindSettings
import com.intellij.find.actions.ShowUsagesAction
import com.intellij.find.impl.FindInProjectUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parents
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import com.intellij.usages.UsageViewPresentation
import java.util.concurrent.atomic.AtomicInteger

class HandleSimpleJavaUtil {

    companion object {
        fun hand(project : Project, method : PsiMethod) : Boolean {
            // 查找是否有地方api调用了这个方法请求路径
            val usageList = mutableListOf<Usage>()
            val searchScope = GlobalSearchScope.projectScope(project)
            val findModel = FindManager.getInstance(project).findInProjectModel
            findModel.directoryName = null
            findModel.isGlobal = false
            findModel.isReplaceState = false
            findModel.isProjectScope = false
            findModel.moduleName = null
            findModel.isWithSubdirectories = true
            findModel.isMultipleFiles = true
            // 设置了排除注释，但仍然没排除，应该没识别出注释，因为如果设置仅注释就查不出来
            // 考虑stringToFind正则表达式
            findModel.searchContext = FindModel.SearchContext.EXCEPT_COMMENTS
            findModel.fileFilter = "*.java"
            val findSettings = FindSettings.getInstance()
            //搜索范围
            findSettings.defaultScopeName = "Directory"
            val myUsageViewPresentation = UsageViewPresentation()
            val processPresentation = FindInProjectUtil.setupProcessPresentation(project!!, myUsageViewPresentation!!)
            val filesToScanInitially: Set<VirtualFile?> = LinkedHashSet()
            val resultsCount = AtomicInteger()
            val state = ModalityState.current()

            // 查找是否有反射调用了这个方法名
            val reflectName = "\"" + method.name + "\""
            findModel.stringToFind = reflectName
            FindInProjectUtil.findUsages(findModel, project, processPresentation, filesToScanInitially) { info ->
                val usage = UsageInfo2UsageAdapter.CONVERTER.`fun`(info)
                val usageInfo = usage as UsageInfo2UsageAdapter
                val psiElement = usageInfo.element
                val targetElement = psiElement?.containingFile?.findElementAt(usage.navigationOffset)
                for (parent in targetElement?.parents!!) {
                    if (parent is PsiAnnotation) {
                        // 注解里可能是controller，提前退出，不算反射调用
                        break
                    }
                    if (parent is PsiJavaFile) {
                        usageList.add(usage)
                        break
                    }
                }
                val continueSearch = resultsCount.incrementAndGet() < ShowUsagesAction.getUsagesPageSize()
                return@findUsages continueSearch
            }
            if (usageList.size > 0) {
                return true
            }

            // 查找是否有直接引用调用了这个方法
            val directReferences = ReferencesSearch.search(method, searchScope).findAll()
            if (directReferences.isNotEmpty()) {
                return true
            }
            return false
        }
    }

}