/**
 * Utility for parsing and resolving Spring property keys from various file formats.
 *
 * @author jonilchan
 */
package com.github.applicationjump.util

import com.intellij.lang.properties.psi.Property
import org.jetbrains.yaml.psi.YAMLKeyValue

object PropertyKeyUtil {

    private val PLACEHOLDER_PATTERN = Regex("""\$\{([^}:]+)(?::([^}]*))?}""")

    /**
     * Extract the full dotted key from a YAML key-value element by walking up the parent chain.
     * e.g. for `url: jdbc:mysql://...` nested under `spring.datasource`, returns `spring.datasource.url`.
     */
    fun getFullYamlKey(element: YAMLKeyValue): String {
        val parts = mutableListOf<String>()
        var current: YAMLKeyValue? = element
        while (current != null) {
            current.name?.let { parts.add(0, it) }
            current = current.parent?.parent as? YAMLKeyValue
        }
        return parts.joinToString(".")
    }

    /**
     * Get the property key from a .properties file element.
     */
    fun getPropertyKey(property: Property): String? {
        return property.key
    }

    /**
     * Extract property keys from a @Value annotation string.
     * e.g. `"${spring.datasource.url}"` -> listOf("spring.datasource.url")
     * e.g. `"${server.port:8080}"` -> listOf("server.port")
     * e.g. `"${a.b}-${c.d}"` -> listOf("a.b", "c.d")
     */
    fun extractKeysFromValueAnnotation(value: String): List<String> {
        return PLACEHOLDER_PATTERN.findAll(value).map { it.groupValues[1].trim() }.toList()
    }

    /**
     * Extract the single property key from a placeholder expression like `${key}` or `${key:default}`.
     */
    fun extractKeyFromPlaceholder(placeholder: String): String? {
        val match = PLACEHOLDER_PATTERN.find(placeholder) ?: return null
        return match.groupValues[1].trim()
    }

    /**
     * Check if a string contains Spring property placeholders.
     */
    fun containsPlaceholder(value: String): Boolean {
        return PLACEHOLDER_PATTERN.containsMatchIn(value)
    }

    /**
     * Build full property keys from a @ConfigurationProperties prefix and field names.
     * e.g. prefix = "spring.datasource", fields = ["url", "username"] -> ["spring.datasource.url", "spring.datasource.username"]
     */
    fun buildConfigPropertiesKeys(prefix: String, fieldNames: List<String>): List<String> {
        val normalizedPrefix = prefix.trimEnd('.')
        return fieldNames.map { "$normalizedPrefix.$it" }
    }

    /**
     * Convert a camelCase field name to kebab-case (Spring relaxed binding).
     * e.g. "maxPoolSize" -> "max-pool-size"
     */
    fun camelToKebab(name: String): String {
        return name.replace(Regex("([a-z])([A-Z])")) { match ->
            "${match.groupValues[1]}-${match.groupValues[2].lowercase()}"
        }
    }

    /**
     * Check if two property keys match under Spring's relaxed binding rules.
     * "spring.datasource.max-pool-size" matches "spring.datasource.maxPoolSize"
     */
    fun keysMatch(key1: String, key2: String): Boolean {
        if (key1 == key2) return true
        val normalized1 = normalizeKey(key1)
        val normalized2 = normalizeKey(key2)
        return normalized1 == normalized2
    }

    /**
     * Normalize a property key for comparison (lowercase, replace separators).
     */
    fun normalizeKey(key: String): String {
        return key.lowercase()
            .replace('_', '.')
            .replace('-', '.')
            .replace(Regex("\\.+"), ".")
    }
}
