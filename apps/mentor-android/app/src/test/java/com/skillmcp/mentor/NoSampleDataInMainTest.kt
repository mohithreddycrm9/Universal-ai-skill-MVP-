package com.skillmcp.mentor

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Shipped code must not contain demo/sample content; samples live in src/test only. */
class NoSampleDataInMainTest {
    private val banned =
        listOf(
            Regex("""(?i)lorem ipsum"""),
            Regex("""(?i)\(example\)"""),
            Regex("""(?i)@example\.(com|org)"""),
            Regex("""(?i)https?://(www\.)?example\.(com|org)"""),
            Regex("""(?i)\b(john|jane) doe\b"""),
            Regex("""(?i)"(sample|demo|fake|dummy) (chat|message|conversation|user|data)"""),
        )

    @Test
    fun mainSourcesHaveNoSampleContent() {
        val roots = listOf(File("src/main/java"), File("src/main/res"))
        val problems =
            roots.flatMap { root ->
                root.walkTopDown().filter { it.isFile && (it.extension == "kt" || it.extension == "xml") }.flatMap { file ->
                    file.readLines().mapIndexedNotNull { i, line ->
                        banned.firstOrNull { it.containsMatchIn(line) }?.let { "${file.path}:${i + 1}: ${line.trim()}" }
                    }
                }.toList()
            }
        assertEquals(problems.joinToString("\n"), 0, problems.size)
    }

    @Test
    fun previewSampleDataIsNotInMain() {
        assertEquals(false, File("src/main/java/com/skillmcp/mentor/ui/preview").exists())
    }
}
