package org.jetbrains.plugins.template.dialogs

import com.intellij.ide.util.TreeFileChooserFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.plugins.template.core.HandleSimpleJavaUtil
import java.awt.GridLayout
import javax.swing.*

class DeleteJavaDialog(anActionEvent: AnActionEvent) : DialogWrapper(true) {

    private val anActionEvent: AnActionEvent
    private val project : Project
    private lateinit var selectedPsiFile : PsiFile
    init {
        title = "精简Controller"
        this.anActionEvent = anActionEvent
        this.project = anActionEvent.getData(PlatformDataKeys.PROJECT)!!
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(1, 3))
        panel.isVisible = true
        val javaChooseLabel = JLabel("Java类")
        panel.add(javaChooseLabel)
        val javaChooseText = JTextField(20)
        javaChooseText.isEnabled = false
        panel.add(javaChooseText)
        val javaChooseButton = JButton("choose")
        javaChooseButton.addActionListener {
            val psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE)
            // 类选择器
            val chooser = TreeFileChooserFactory.getInstance(project)
                .createFileChooser(
                    "文件选择",
                    psiFile,
                    null,
                    null,
                )
            chooser.showDialog()
            val choosePsiFile = chooser.selectedFile
            if (choosePsiFile != null) {
                selectedPsiFile = choosePsiFile
                javaChooseText.text = choosePsiFile.name
            }
        }
        panel.add(javaChooseButton);
        return panel
    }

    override fun doOKAction() {
        if (!::selectedPsiFile.isInitialized || selectedPsiFile.fileType.name != "JAVA") {
            Messages.showWarningDialog("请选择Java类", "警告")
            return
        }
        val deletedList = mutableListOf<String>()
        if (selectedPsiFile is PsiJavaFile) {
            val psiJavaFile = selectedPsiFile as PsiJavaFile
            val elements = psiJavaFile.children
            for (element in elements) {
                if (element is PsiClass) {
                    val psiClass = element as PsiClass
                    val methods = psiClass.methods
                    for (method in methods) {
                        // 遍历类中的方法
                        if (!HandleSimpleJavaUtil.hand(project, method)) {
                            deletedList.add(method.name)
                            WriteCommandAction.runWriteCommandAction(project) {
                                method.delete()
                            }
                        }
                    }
                }
            }
        } else {
            Messages.showWarningDialog("请选择Java类", "警告")
            return
        }
        Messages.showWarningDialog("删除了${deletedList.size}个方法，分别是${deletedList.joinToString(",")}", "警告")
        super.doOKAction()
    }

}