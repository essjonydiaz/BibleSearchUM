package com.example.biblesearchum.model

import com.google.gson.annotations.SerializedName

data class BibleBook(
    @SerializedName("bookid") val bookId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("chapters") val chapters: Int
)

data class BibleVerse(
    @SerializedName("pk") val pk: Int,
    @SerializedName("verse") val verse: Int,
    @SerializedName("text") val text: String
)

private val HTML_TAG = Regex("<[^>]+>")
private val WHITESPACE = Regex("\\s+")

/**
 * La API devuelve algunos versículos con etiquetas HTML (p. ej. `<br>`) y espacios dobles.
 * Esta función los convierte en texto plano listo para mostrar.
 */
fun String.toPlainVerseText(): String =
    replace(HTML_TAG, " ")
        .replace(WHITESPACE, " ")
        .trim()
