package org.jetbrains.plugins.template.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.plugins.template.dialogs.DeleteJavaDialog

class DeleteJavaAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        DeleteJavaDialog(e).show()
    }

}