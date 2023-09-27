package org.jetbrains.plugins.template.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.plugins.template.dialogs.CustomDialog

class GujieActionDemo : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        CustomDialog(e).show()
    }

}