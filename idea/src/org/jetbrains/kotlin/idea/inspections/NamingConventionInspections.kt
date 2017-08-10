/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.ui.EditorTextField
import com.siyeh.ig.fixes.RenameFix
import org.intellij.lang.regexp.RegExpFileType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtVisitorVoid
import java.awt.BorderLayout
import javax.swing.JPanel

abstract class NamingConventionInspection : AbstractKotlinInspection() {
    private var nameRegex: Regex = defaultNamePattern.toRegex()
    var namePattern: String = defaultNamePattern
        set(value) {
            field = value
            nameRegex = value.toRegex()
        }

    abstract val defaultNamePattern: String

    protected fun verifyName(element: PsiNameIdentifierOwner, holder: ProblemsHolder) {
        val name = element.name
        val nameIdentifier = element.nameIdentifier
        if (name != null && nameIdentifier != null && !nameRegex.matches(name)) {
            holder.registerProblem(element.nameIdentifier!!,
                                   "Class name <code>#ref</code> doesn''t match regex ''$namePattern'' #loc",
                                   RenameFix())
        }
    }

    override fun createOptionsPanel() = NamingConventionOptionsPanel(this)
}

class ClassNameInspection : NamingConventionInspection() {
    override val defaultNamePattern: String
        get() = "[A-Z][A-Za-z\\d]*"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                verifyName(classOrObject, holder)
            }
        }
    }
}

class NamingConventionOptionsPanel(owner: NamingConventionInspection) : JPanel() {
    init {
        layout = BorderLayout()

        val regexField = EditorTextField(owner.namePattern, null, RegExpFileType.INSTANCE)
        regexField.document.addDocumentListener(object : DocumentAdapter() {
            override fun documentChanged(e: DocumentEvent?) {
                owner.namePattern = regexField.text
            }
        })
        val labeledComponent = LabeledComponent.create(regexField, "Pattern:", BorderLayout.WEST)
        add(labeledComponent, BorderLayout.CENTER)
    }
}
