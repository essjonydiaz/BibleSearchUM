package com.example.biblesearchum.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VerseTextTest {

    @Test
    fun `quita espacios dobles`() {
        val raw = "Porque de tal manera amó Dios al mundo,  que ha dado a su Hijo unigénito,  "
        assertEquals(
            "Porque de tal manera amó Dios al mundo, que ha dado a su Hijo unigénito,",
            raw.toPlainVerseText()
        )
    }

    @Test
    fun `reemplaza etiquetas html por espacios`() {
        assertEquals("Y dijo Dios: Sea la luz", "Y dijo Dios:<br>Sea la luz".toPlainVerseText())
        assertEquals("texto en cursiva", "texto <i>en</i> cursiva".toPlainVerseText())
    }
}
