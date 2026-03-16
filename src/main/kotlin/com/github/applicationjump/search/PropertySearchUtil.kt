/**
 * Searches for Spring property key usages and definitions across the project.
 * Uses FileBasedIndex for fast lookup when available, falls back to PSI traversal.
 *
 * @author jonilchan
 */
package com.github.applicationjump.search

import com.github.applicationjump.index.SpringPropertyUsageIndex
import com.github.applicationjump.util.PropertyKeyUtil
import com.github.applicationjump.util.SpringAnnotationUtil
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue

object PropertySearchUtil {

    /**
     * Find all code elements that reference a given property key.
     * Uses the index to narrow down files, then performs precise PSI resolution.
     */
    fun findCodeReferencesForKey(project: Project, propertyKey: String): List<PsiElement> {
        val normalizedKey = PropertyKeyUtil.normalizeKey(propertyKey)
        val scope = GlobalSearchScope.projectScope(project)
        val results = mutableListOf<PsiElement>()
        val visitedFiles = mutableSetOf<String>()

        val indexedFiles = FileBasedIndex.getInstance()
            .getContainingFiles(SpringPropertyUsageIndex.NAME, normalizedKey, scope)

        for (virtualFile in indexedFiles) {
            visitedFiles.add(virtualFile.path)
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: continue
            collectCodeReferences(psiFile, propertyKey, results)
        }

        if (results.isEmpty()) {
            val javaFiles = FileTypeIndex.getFiles(com.intellij.ide.highlighter.JavaFileType.INSTANCE, scope)
            for (virtualFile in javaFiles) {
                if (virtualFile.path in visitedFiles) continue
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: continue
                collectCodeReferences(psiFile, propertyKey, results)
            }
        }

        return results
    }

    /**
     * Find all configuration file definitions for a given property key.
     */
    fun findConfigDefinitionsForKey(project: Project, propertyKey: String): List<PsiElement> {
        val results = mutableListOf<PsiElement>()
        findPropertiesDefinitions(project, propertyKey, results)
        findYamlDefinitions(project, propertyKey, results)
        return results
    }

    private fun findPropertiesDefinitions(project: Project, key: String, results: MutableList<PsiElement>) {
        val scope = GlobalSearchScope.projectScope(project)
        val files = FileTypeIndex.getFiles(PropertiesFileType.INSTANCE, scope)

        for (virtualFile in files) {
            if (!isSpringConfigFile(virtualFile.name)) continue
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? PropertiesFile ?: continue
            for (property in psiFile.properties) {
                val propKey = property.key ?: continue
                if (PropertyKeyUtil.keysMatch(propKey, key)) {
                    (property as? PsiElement)?.let { results.add(it) }
                }
            }
        }
    }

    private fun findYamlDefinitions(project: Project, key: String, results: MutableList<PsiElement>) {
        val scope = GlobalSearchScope.projectScope(project)
        val files = FileTypeIndex.getFiles(YAMLFileType.YML, scope)

        for (virtualFile in files) {
            if (!isSpringConfigFile(virtualFile.name)) continue
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? YAMLFile ?: continue
            val keyValues = PsiTreeUtil.findChildrenOfType(psiFile, YAMLKeyValue::class.java)
            for (kv in keyValues) {
                val fullKey = PropertyKeyUtil.getFullYamlKey(kv)
                if (PropertyKeyUtil.keysMatch(fullKey, key)) {
                    results.add(kv)
                }
            }
        }
    }

    private fun collectCodeReferences(psiFile: PsiFile, propertyKey: String, results: MutableList<PsiElement>) {
        val annotations = PsiTreeUtil.findChildrenOfType(psiFile, PsiAnnotation::class.java)
        for (annotation in annotations) {
            when (annotation.qualifiedName) {
                SpringAnnotationUtil.VALUE_ANNOTATION -> {
                    val keys = SpringAnnotationUtil.extractValueAnnotationKeys(annotation)
                    if (keys.any { PropertyKeyUtil.keysMatch(it, propertyKey) }) {
                        results.add(annotation)
                    }
                }
                SpringAnnotationUtil.CONFIG_PROPERTIES_ANNOTATION -> {
                    val prefix = SpringAnnotationUtil.extractConfigPropertiesPrefix(annotation) ?: continue
                    val psiClass = PsiTreeUtil.getParentOfType(annotation, PsiClass::class.java) ?: continue
                    val fieldNames = SpringAnnotationUtil.getConfigPropertiesFields(psiClass)
                    val allKeys = PropertyKeyUtil.buildConfigPropertiesKeys(prefix, fieldNames) +
                            PropertyKeyUtil.buildConfigPropertiesKeys(prefix, fieldNames.map { PropertyKeyUtil.camelToKebab(it) })
                    if (allKeys.any { PropertyKeyUtil.keysMatch(it, propertyKey) }) {
                        results.add(annotation)
                    }
                    if (PropertyKeyUtil.normalizeKey(propertyKey).startsWith(PropertyKeyUtil.normalizeKey(prefix))) {
                        if (!results.contains(annotation)) {
                            results.add(annotation)
                        }
                    }
                }
                SpringAnnotationUtil.CONDITIONAL_ON_PROPERTY -> {
                    val keys = SpringAnnotationUtil.extractConditionalOnPropertyKeys(annotation)
                    if (keys.any { PropertyKeyUtil.keysMatch(it, propertyKey) }) {
                        results.add(annotation)
                    }
                }
            }
        }

        val methodCalls = PsiTreeUtil.findChildrenOfType(psiFile, PsiMethodCallExpression::class.java)
        for (call in methodCalls) {
            val key = SpringAnnotationUtil.extractEnvironmentGetPropertyKey(call) ?: continue
            if (PropertyKeyUtil.keysMatch(key, propertyKey)) {
                results.add(call)
            }
        }
    }

    fun isSpringConfigFile(fileName: String): Boolean {
        val name = fileName.lowercase()
        return (name.startsWith("application") || name.startsWith("bootstrap")) && (
                name.endsWith(".properties") ||
                name.endsWith(".yml") ||
                name.endsWith(".yaml")
        )
    }
}
