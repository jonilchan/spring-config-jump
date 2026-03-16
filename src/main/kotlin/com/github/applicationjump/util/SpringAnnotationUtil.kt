/**
 * Utility for detecting and parsing Spring annotations related to property references.
 *
 * @author jonilchan
 */
package com.github.applicationjump.util

import com.intellij.psi.*

object SpringAnnotationUtil {

    const val VALUE_ANNOTATION = "org.springframework.beans.factory.annotation.Value"
    const val CONFIG_PROPERTIES_ANNOTATION = "org.springframework.boot.context.properties.ConfigurationProperties"
    const val CONDITIONAL_ON_PROPERTY = "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
    const val ENVIRONMENT_CLASS = "org.springframework.core.env.Environment"

    /**
     * Extract property keys referenced in a @Value annotation.
     * Returns empty list if the annotation is not @Value or contains no placeholders.
     */
    fun extractValueAnnotationKeys(annotation: PsiAnnotation): List<String> {
        if (annotation.qualifiedName != VALUE_ANNOTATION) return emptyList()
        val value = annotation.findAttributeValue("value") ?: return emptyList()
        val text = resolveAnnotationStringValue(value) ?: return emptyList()
        return PropertyKeyUtil.extractKeysFromValueAnnotation(text)
    }

    /**
     * Extract prefix from @ConfigurationProperties annotation.
     */
    fun extractConfigPropertiesPrefix(annotation: PsiAnnotation): String? {
        if (annotation.qualifiedName != CONFIG_PROPERTIES_ANNOTATION) return null
        val prefixAttr = annotation.findAttributeValue("prefix")
            ?: annotation.findAttributeValue("value")
            ?: return null
        return resolveAnnotationStringValue(prefixAttr)
    }

    /**
     * Extract property keys from @ConditionalOnProperty annotation.
     * Handles both `name` and `prefix` + `name` combinations.
     */
    fun extractConditionalOnPropertyKeys(annotation: PsiAnnotation): List<String> {
        if (annotation.qualifiedName != CONDITIONAL_ON_PROPERTY) return emptyList()

        val prefix = annotation.findAttributeValue("prefix")
            ?.let { resolveAnnotationStringValue(it) }
            ?.takeIf { it.isNotBlank() }

        val names = extractAnnotationArrayValue(annotation, "name")
            .ifEmpty { extractAnnotationArrayValue(annotation, "value") }

        if (names.isEmpty()) return emptyList()

        return if (prefix != null) {
            val normalizedPrefix = prefix.trimEnd('.')
            names.map { "$normalizedPrefix.$it" }
        } else {
            names
        }
    }

    /**
     * Check if a method call is Environment.getProperty() and extract the property key.
     */
    fun extractEnvironmentGetPropertyKey(call: PsiMethodCallExpression): String? {
        val method = call.resolveMethod() ?: return null
        val containingClass = method.containingClass ?: return null

        if (method.name != "getProperty") return null

        val qualifiedName = containingClass.qualifiedName ?: return null
        if (qualifiedName != ENVIRONMENT_CLASS && !isSubclassOf(containingClass, ENVIRONMENT_CLASS)) {
            return null
        }

        val args = call.argumentList.expressions
        if (args.isEmpty()) return null

        val firstArg = args[0]
        return resolveStringLiteral(firstArg)
    }

    /**
     * Check if a PsiElement is inside a @Value annotation string literal.
     */
    fun isInsideValueAnnotation(element: PsiElement): Boolean {
        val literal = element.parent as? PsiLiteralExpression ?: return false
        val nameValuePair = literal.parent as? PsiNameValuePair ?: return false
        val annotation = nameValuePair.parent?.parent as? PsiAnnotation ?: return false
        return annotation.qualifiedName == VALUE_ANNOTATION
    }

    /**
     * Find the @ConfigurationProperties annotation on a class, if present.
     */
    fun findConfigPropertiesAnnotation(psiClass: PsiClass): PsiAnnotation? {
        return psiClass.getAnnotation(CONFIG_PROPERTIES_ANNOTATION)
    }

    /**
     * Get all field names from a class annotated with @ConfigurationProperties.
     */
    fun getConfigPropertiesFields(psiClass: PsiClass): List<String> {
        return psiClass.fields
            .filter { !it.hasModifierProperty(PsiModifier.STATIC) }
            .mapNotNull { it.name }
    }

    private fun resolveAnnotationStringValue(value: PsiAnnotationMemberValue): String? {
        return when (value) {
            is PsiLiteralExpression -> value.value as? String
            is PsiReferenceExpression -> {
                val resolved = value.resolve()
                if (resolved is PsiField && resolved.hasModifierProperty(PsiModifier.FINAL)) {
                    (resolved.initializer as? PsiLiteralExpression)?.value as? String
                } else null
            }
            else -> null
        }
    }

    private fun extractAnnotationArrayValue(annotation: PsiAnnotation, attrName: String): List<String> {
        val attr = annotation.findAttributeValue(attrName) ?: return emptyList()
        return when (attr) {
            is PsiArrayInitializerMemberValue -> {
                attr.initializers.mapNotNull { resolveAnnotationStringValue(it) }
            }
            is PsiLiteralExpression -> {
                listOfNotNull(attr.value as? String)
            }
            else -> emptyList()
        }
    }

    private fun resolveStringLiteral(expression: PsiExpression): String? {
        return when (expression) {
            is PsiLiteralExpression -> expression.value as? String
            is PsiReferenceExpression -> {
                val resolved = expression.resolve()
                if (resolved is PsiField && resolved.hasModifierProperty(PsiModifier.FINAL)) {
                    (resolved.initializer as? PsiLiteralExpression)?.value as? String
                } else null
            }
            else -> null
        }
    }

    private fun isSubclassOf(
        psiClass: PsiClass,
        qualifiedName: String,
        visited: MutableSet<String> = mutableSetOf()
    ): Boolean {
        val classQn = psiClass.qualifiedName ?: return false
        if (!visited.add(classQn)) return false
        return psiClass.superTypes.any { superType ->
            val resolved = superType.resolve() ?: return@any false
            resolved.qualifiedName == qualifiedName || isSubclassOf(resolved, qualifiedName, visited)
        }
    }
}
