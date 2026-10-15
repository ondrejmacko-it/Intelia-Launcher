package dev.intelia.launcher.search

import java.text.Normalizer
import java.util.Locale

/** Local, diacritics-insensitive ranking. Lower score ranks higher; ties keep input order. */
object AppSearch {
    private val marks = Regex("\\p{Mn}+")
    private val separators = Regex("[\\s\\p{Z}\\p{P}]+")

    fun fold(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(marks, "").lowercase(Locale.ROOT).trim()

    fun score(folded: String, query: String): Int? {
        if (query.isEmpty()) return null
        if (folded == query) return 0
        if (folded.startsWith(query)) return 1
        val words = folded.split(separators).filter(String::isNotEmpty)
        val parts = query.split(separators).filter(String::isNotEmpty)
        if (parts.size > 1) {
            return if (parts.all { part -> words.any { it.startsWith(part) } }) 2 else null
        }
        if (words.any { it.startsWith(query) }) return 2
        if (query.length >= 2 && words.size >= 2 && words.joinToString("") { it.take(1) }.startsWith(query)) return 3
        if (query.length >= 2 && folded.contains(query)) return 4
        return null
    }

    fun <T> rank(items: List<T>, query: String, key: (T) -> String): List<T> {
        val folded = fold(query)
        if (folded.isEmpty()) return emptyList()
        return items.mapNotNull { item -> score(key(item), folded)?.let { it to item } }
            .sortedBy { it.first }.map { it.second }
    }
}
