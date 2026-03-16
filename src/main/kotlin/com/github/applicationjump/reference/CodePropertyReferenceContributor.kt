/**
 * Contributes references from Java/Kotlin code (inside @Value, Environment.getProperty(), etc.)
 * to configuration file property definitions, enabling Ctrl+Click navigation.
 *
 * @author jonilchan
 */
package com.github.applicationjump.reference

import com.github.applicationjump.search.PropertySearchUtil
import com.github.applicationjump.util.PropertyKeyUtil
import com.github.applicationjump.util.SpringAnnotationUtil
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext

class CodePropertyReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiLiteralExpression::class.java),
            CodePropertyReferenceProvider()
        )
    }
}

private class CodePropertyReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is PsiLiteralExpression) return PsiReference.EMPTY_ARRAY
        val value = element.value as? String ?: return PsiReference.EMPTY_ARRAY

        return when {
            isInValueAnnotation(element) -> createValueAnnotationReferences(element, value)
            isInConditionalOnProperty(element) -> createDirectKeyReference(element, value)
            isInConfigurationProperties(element) -> createDirectKeyReference(element, value)
            isInEnvironmentGetProperty(element) -> createDirectKeyReference(element, value)
            else -> PsiReference.EMPTY_ARRAY
        }
    }

    private fun createValueAnnotationReferences(element: PsiLiteralExpression, value: String): Array<PsiReference> {
        val keys = PropertyKeyUtil.extractKeysFromValueAnnotation(value)
        if (keys.isEmpty()) return PsiReference.EMPTY_ARRAY

        val text = element.text
        return keys.mapNotNull { key ->
            val keyStart = text.indexOf(key)
            if (keyStart < 0) return@mapNotNull null
            val range = TextRange(keyStart, keyStart + key.length)
            CodeToConfigPropertyReference(element, range, key)
        }.toTypedArray()
    }

    private fun createDirectKeyReference(element: PsiLiteralExpression, value: String): Array<PsiReference> {
        if (value.isBlank()) return PsiReference.EMPTY_ARRAY
        val range = TextRange(1, value.length + 1)
        return arrayOf(CodeToConfigPropertyReference(element, range, value))
    }

    private fun findEnclosingAnnotationQualifiedName(element: PsiLiteralExpression): String? {
        val nameValuePair = element.parent as? PsiNameValuePair
            ?: (element.parent as? PsiArrayInitializerMemberValue)?.parent as? PsiNameValuePair
            ?: return null
        val annotation = nameValuePair.parent?.parent as? PsiAnnotation ?: return null
        return annotation.qualifiedName
    }

    private fun isInValueAnnotation(element: PsiLiteralExpression): Boolean =
        findEnclosingAnnotationQualifiedName(element) == SpringAnnotationUtil.VALUE_ANNOTATION

    private fun isInConditionalOnProperty(element: PsiLiteralExpression): Boolean =
        findEnclosingAnnotationQualifiedName(element) == SpringAnnotationUtil.CONDITIONAL_ON_PROPERTY

    private fun isInConfigurationProperties(element: PsiLiteralExpression): Boolean =
        findEnclosingAnnotationQualifiedName(element) == SpringAnnotationUtil.CONFIG_PROPERTIES_ANNOTATION

    private fun isInEnvironmentGetProperty(element: PsiLiteralExpression): Boolean {
        val argList = element.parent as? PsiExpressionList ?: return false
        val methodCall = argList.parent as? PsiMethodCallExpression ?: return false
        return SpringAnnotationUtil.extractEnvironmentGetPropertyKey(methodCall) != null
    }
}

private class CodeToConfigPropertyReference(
    element: PsiElement,
    range: TextRange,
    private val propertyKey: String
) : PsiReferenceBase<PsiElement>(element, range, true) {

    override fun resolve(): PsiElement? {
        val targets = PropertySearchUtil.findConfigDefinitionsForKey(element.project, propertyKey)
        return targets.firstOrNull()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
