/**
 * Contributes references from YAML configuration file keys to their code usages,
 * enabling Ctrl+Click navigation from YAML keys to code.
 *
 * @author jonilchan
 */
package com.github.applicationjump.reference

import com.github.applicationjump.search.PropertySearchUtil
import com.github.applicationjump.util.PropertyKeyUtil
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLKeyValue

class YamlPropertyReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLKeyValue::class.java),
            YamlPropertyKeyReferenceProvider()
        )
    }
}

private class YamlPropertyKeyReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is YAMLKeyValue) return PsiReference.EMPTY_ARRAY

        val fileName = element.containingFile?.name ?: return PsiReference.EMPTY_ARRAY
        if (!PropertySearchUtil.isSpringConfigFile(fileName)) return PsiReference.EMPTY_ARRAY

        val key = element.key ?: return PsiReference.EMPTY_ARRAY
        val fullKey = PropertyKeyUtil.getFullYamlKey(element)
        val range = TextRange(0, key.textLength)

        return arrayOf(YamlSpringPropertyReference(key, range, fullKey))
    }
}

private class YamlSpringPropertyReference(
    element: PsiElement,
    range: TextRange,
    private val fullPropertyKey: String
) : PsiReferenceBase<PsiElement>(element, range, true) {

    override fun resolve(): PsiElement? {
        val targets = PropertySearchUtil.findCodeReferencesForKey(element.project, fullPropertyKey)
        return targets.firstOrNull()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
