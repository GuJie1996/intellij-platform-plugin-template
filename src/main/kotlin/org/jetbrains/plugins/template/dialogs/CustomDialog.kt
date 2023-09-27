package org.jetbrains.plugins.template.dialogs

import com.intellij.ide.util.TreeFileChooserFactory
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import org.jetbrains.plugins.template.core.HandleJSProjectUtil
import org.jetbrains.plugins.template.core.HandleJavaProjectUtil
import java.awt.GridLayout
import javax.swing.*

class CustomDialog(anActionEvent: AnActionEvent) : DialogWrapper(true) {
    private val anActionEvent: AnActionEvent
    private val project : Project
    private lateinit var jsProjectPathStr : String
    private lateinit var selectedPsiFile : PsiFile
    init {
        title = "精简Controller"
        this.anActionEvent = anActionEvent
        this.project = anActionEvent.getData(PlatformDataKeys.PROJECT)!!
        init()
    }
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(2, 3))
        panel.isVisible = true
        val jsProjectChooseLabel = JLabel("前端项目路径")
        panel.add(jsProjectChooseLabel)
        val jsProjectChooseText = JTextField(20)
        jsProjectChooseText.isEnabled = false
        panel.add(jsProjectChooseText)
        val jsProjectChooseButton = JButton("choose")
        jsProjectChooseButton.addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
            val files = FileChooser.chooseFiles(descriptor, project, null)
            for(file in files) {
                jsProjectChooseText.text = file.path
                jsProjectPathStr = file.path
            }
        }
        panel.add(jsProjectChooseButton)
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
        if (!::jsProjectPathStr.isInitialized) {
            Messages.showWarningDialog("请选择前端路径", "警告")
            return
        }
        val deletedList = mutableListOf<String>()
        val deletedMethodList = mutableListOf<PsiMethod>()
        if (selectedPsiFile is PsiJavaFile) {
            val psiJavaFile = selectedPsiFile as PsiJavaFile
            val elements = psiJavaFile.children
            for (element in elements) {
                if (element is PsiClass) {
                    val psiClass = element as PsiClass
                    var path = ""
                    for (annotation in psiClass.annotations) {
                        // 遍历类中的注解
                        if (annotation.qualifiedName == "org.springframework.web.bind.annotation.RequestMapping"
                            || annotation.qualifiedName == "org.springframework.web.bind.annotation.GetMapping"
                            || annotation.qualifiedName == "org.springframework.web.bind.annotation.PostMapping") {
                            val valueAttribute = annotation.findAttributeValue("value")
                            if (valueAttribute != null) {
                                val text = valueAttribute.text
                                path = text
                            }
                        }
                    }
                    val methods = psiClass.methods
                    for (method in methods) {
                        // 遍历类中的方法
                        var methodPath = ""
                        for (annotation in psiClass.annotations) {
                            // 遍历类中的注解
                            if (annotation.qualifiedName == "org.springframework.web.bind.annotation.RequestMapping"
                                || annotation.qualifiedName == "org.springframework.web.bind.annotation.GetMapping"
                                || annotation.qualifiedName == "org.springframework.web.bind.annotation.PostMapping") {
                                val valueAttribute = annotation.findAttributeValue("value")
                                if (valueAttribute != null) {
                                    val text = valueAttribute.text
                                    methodPath = text
                                }
                            }
                        }
                        if (!methodPath.startsWith("/")) {
                            methodPath = "/$methodPath"
                        }
                        val fullPath = path + methodPath
                        if (!HandleJavaProjectUtil.hand(project, fullPath, method) && !HandleJSProjectUtil.hand(jsProjectPathStr, fullPath, project)) {
                            deletedList.add(method.name)
                            deletedMethodList.add(method)
                        }
                    }
                    if (deletedList.size > 0) {
                        WriteCommandAction.runWriteCommandAction(project) {
                            for (needDeleteItem in deletedMethodList) {
                                needDeleteItem.delete()
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
//            selectedPsiFile.accept(object : PsiElementVisitor() {
//                override fun visitElement(element: PsiElement) {
//                    if (element is PsiJavaFile) {
//
//                    }
//                    super.visitElement(element)
//                }
//            })
//            JavaRecursiveElementVisitor
//            HandleJavaProjectUtil.hand(project)
//            HandleJSProjectUtil.hand(jsProjectPathStr, project)
        super.doOKAction()
    }
}