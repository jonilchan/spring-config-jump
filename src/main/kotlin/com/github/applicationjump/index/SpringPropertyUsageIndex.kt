/**
 * FileBasedIndex that indexes Spring property key references across Java source files.
 * This enables fast lookup of which code locations reference a given property key,
 * avoiding full-project PSI traversal on every navigation request.
 *
 * @author jonilchan
 */
package com.github.applicationjump.index

import com.github.applicationjump.util.PropertyKeyUtil
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

class SpringPropertyUsageIndex : FileBasedIndexExtension<String, List<Int>>() {

    companion object {
        val NAME: ID<String, List<Int>> = ID.create("com.github.applicationjump.springPropertyUsage")
        private const val VERSION = 3
    }

    override fun getName(): ID<String, List<Int>> = NAME

    override fun getIndexer(): DataIndexer<String, List<Int>, FileContent> {
        return DataIndexer { inputData ->
            val result = mutableMapOf<String, MutableList<Int>>()
            val text = inputData.contentAsText.toString()

            indexValueAnnotations(text, result)
            indexConfigurationProperties(text, result)
            indexConditionalOnProperty(text, result)
            indexEnvironmentGetProperty(text, result)

            result.mapValues { it.value.toList() }
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<List<Int>> = IntListExternalizer()

    override fun getVersion(): Int = VERSION

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(JavaFileType.INSTANCE)
    }

    override fun dependsOnFileContent(): Boolean = true

    private fun indexValueAnnotations(text: String, result: MutableMap<String, MutableList<Int>>) {
        val pattern = Regex("""@Value\s*\(\s*"([^"]+)"\s*\)""")
        for (match in pattern.findAll(text)) {
            val annotationValue = match.groupValues[1]
            val keys = PropertyKeyUtil.extractKeysFromValueAnnotation(annotationValue)
            val offset = match.range.first
            for (key in keys) {
                result.getOrPut(PropertyKeyUtil.normalizeKey(key)) { mutableListOf() }.add(offset)
            }
        }
    }

    private fun indexConfigurationProperties(text: String, result: MutableMap<String, MutableList<Int>>) {
        val pattern = Regex("""@ConfigurationProperties\s*\(\s*(?:prefix\s*=\s*|value\s*=\s*)?"([^"]+)"\s*\)""")
        for (match in pattern.findAll(text)) {
            val prefix = match.groupValues[1]
            val offset = match.range.first
            result.getOrPut(PropertyKeyUtil.normalizeKey(prefix)) { mutableListOf() }.add(offset)
        }
    }

    private fun indexConditionalOnProperty(text: String, result: MutableMap<String, MutableList<Int>>) {
        val pattern = Regex("""@ConditionalOnProperty\s*\([^)]*(?:name|value)\s*=\s*"([^"]+)"[^)]*\)""")
        for (match in pattern.findAll(text)) {
            val key = match.groupValues[1]
            val offset = match.range.first
            result.getOrPut(PropertyKeyUtil.normalizeKey(key)) { mutableListOf() }.add(offset)
        }
    }

    private fun indexEnvironmentGetProperty(text: String, result: MutableMap<String, MutableList<Int>>) {
        val pattern = Regex("""\.getProperty\s*\(\s*"([^"]+)"\s*[,)]""")
        for (match in pattern.findAll(text)) {
            val key = match.groupValues[1]
            val offset = match.range.first
            result.getOrPut(PropertyKeyUtil.normalizeKey(key)) { mutableListOf() }.add(offset)
        }
    }
}

private class IntListExternalizer : DataExternalizer<List<Int>> {

    override fun save(out: DataOutput, value: List<Int>) {
        out.writeInt(value.size)
        for (v in value) {
            out.writeInt(v)
        }
    }

    override fun read(input: DataInput): List<Int> {
        val size = input.readInt()
        val list = ArrayList<Int>(size)
        repeat(size) {
            list.add(input.readInt())
        }
        return list
    }
}
