/**
 * Provides gutter icons on Spring annotations (@Value, @ConfigurationProperties, etc.)
 * that navigate to property definitions in configuration files.
 *
 * @author jonilchan
 */
package com.github.applicationjump.provider

import com.github.applicationjump.search.PropertySearchUtil
import com.github.applicationjump.util.Icons
import com.github.applicationjump.util.PropertyKeyUtil
import com.github.applicationjump.util.SpringAnnotationUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

class CodeToConfigLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is PsiIdentifier) return
        val parent = element.parent ?: return

        when {
            parent is PsiJavaCodeReferenceElement && parent.parent is PsiAnnotation -> {
                handleAnnotation(element, parent.parent as PsiAnnotation, result)
            }
            parent is PsiAnnotation -> {
                handleAnnotation(element, parent, result)
            }
            parent is PsiReferenceExpression && parent.parent is PsiMethodCallExpression -> {
                handleMethodCallExpression(element, parent.parent as PsiMethodCallExpression, result)
            }
        }
    }

    private fun handleAnnotation(
        identifier: PsiIdentifier,
        annotation: PsiAnnotation,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val keys = when (annotation.qualifiedName) {
            SpringAnnotationUtil.VALUE_ANNOTATION ->
                SpringAnnotationUtil.extractValueAnnotationKeys(annotation)
            SpringAnnotationUtil.CONFIG_PROPERTIES_ANNOTATION -> {
                val prefix = SpringAnnotationUtil.extractConfigPropertiesPrefix(annotation) ?: return
                val psiClass = PsiTreeUtil.getParentOfType(annotation, PsiClass::class.java) ?: return
                val fieldNames = SpringAnnotationUtil.getConfigPropertiesFields(psiClass)
                PropertyKeyUtil.buildConfigPropertiesKeys(prefix, fieldNames)
            }
            SpringAnnotationUtil.CONDITIONAL_ON_PROPERTY ->
                SpringAnnotationUtil.extractConditionalOnPropertyKeys(annotation)
            else -> return
        }

        if (keys.isEmpty()) return

        val project = identifier.project
        val targets = keys.flatMap { PropertySearchUtil.findConfigDefinitionsForKey(project, it) }.distinct()
        if (targets.isEmpty()) return

        val tooltip = if (keys.size == 1) {
            "Navigate to config definition of '${keys.first()}'"
        } else {
            "Navigate to config definitions (${keys.size} keys)"
        }

        val builder = NavigationGutterIconBuilder.create(Icons.SPRING_PROPERTY)
            .setTargets(targets)
            .setTooltipText(tooltip)

        result.add(builder.createLineMarkerInfo(identifier))
    }

    private fun handleMethodCallExpression(
        identifier: PsiIdentifier,
        methodCall: PsiMethodCallExpression,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val key = SpringAnnotationUtil.extractEnvironmentGetPropertyKey(methodCall) ?: return

        val project = identifier.project
        val targets = PropertySearchUtil.findConfigDefinitionsForKey(project, key)
        if (targets.isEmpty()) return

        val builder = NavigationGutterIconBuilder.create(Icons.SPRING_PROPERTY)
            .setTargets(targets)
            .setTooltipText("Navigate to config definition of '$key'")

        result.add(builder.createLineMarkerInfo(identifier))
    }
}
