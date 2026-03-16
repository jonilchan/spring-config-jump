/**
 * Provides gutter icons in Spring configuration files (.properties / .yml)
 * that navigate to code references of the property key.
 *
 * @author jonilchan
 */
package com.github.applicationjump.provider

import com.github.applicationjump.search.PropertySearchUtil
import com.github.applicationjump.util.Icons
import com.github.applicationjump.util.PropertyKeyUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl
import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLKeyValue

class ConfigToCodeLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val propertyKey: String
        val markerElement: PsiElement

        when (element) {
            is PropertyKeyImpl -> {
                val property = element.parent as? com.intellij.lang.properties.psi.Property ?: return
                if (!PropertySearchUtil.isSpringConfigFile(element.containingFile?.name ?: "")) return
                propertyKey = property.key ?: return
                markerElement = element
            }
            is YAMLKeyValue -> {
                if (!PropertySearchUtil.isSpringConfigFile(element.containingFile?.name ?: "")) return
                val key = element.key ?: return
                propertyKey = PropertyKeyUtil.getFullYamlKey(element)
                markerElement = key
            }
            else -> return
        }

        val project = element.project
        val targets = PropertySearchUtil.findCodeReferencesForKey(project, propertyKey)
        if (targets.isEmpty()) return

        val builder = NavigationGutterIconBuilder.create(Icons.SPRING_PROPERTY)
            .setTargets(targets)
            .setTooltipText("Navigate to code references of '$propertyKey'")

        result.add(builder.createLineMarkerInfo(markerElement))
    }
}
