/**
 * Contributes references from .properties file keys to their code usages,
 * enabling Ctrl+Click navigation from config keys to code.
 *
 * @author jonilchan
 */
package com.github.applicationjump.reference

import com.github.applicationjump.search.PropertySearchUtil
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext

class PropertyReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PropertyKeyImpl::class.java),
            PropertyKeyReferenceProvider()
        )
    }
}

private class PropertyKeyReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is PropertyKeyImpl) return PsiReference.EMPTY_ARRAY

        val property = element.parent as? com.intellij.lang.properties.psi.Property ?: return PsiReference.EMPTY_ARRAY
        val fileName = element.containingFile?.name ?: return PsiReference.EMPTY_ARRAY
        if (!PropertySearchUtil.isSpringConfigFile(fileName)) return PsiReference.EMPTY_ARRAY

        val key = property.key ?: return PsiReference.EMPTY_ARRAY
        val range = TextRange(0, element.textLength)

        return arrayOf(SpringPropertyReference(element, range, key))
    }
}

class SpringPropertyReference(
    element: PsiElement,
    range: TextRange,
    private val propertyKey: String
) : PsiReferenceBase<PsiElement>(element, range, true) {

    override fun resolve(): PsiElement? {
        val targets = PropertySearchUtil.findCodeReferencesForKey(element.project, propertyKey)
        return targets.firstOrNull()
    }

    override fun getVariants(): Array<Any> = emptyArray()

    override fun isReferenceTo(target: PsiElement): Boolean {
        val resolved = resolve() ?: return false
        val manager = element.manager
        return manager.areElementsEquivalent(resolved, target)
    }
}
