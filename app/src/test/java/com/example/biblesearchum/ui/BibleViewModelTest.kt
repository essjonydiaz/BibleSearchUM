package com.example.biblesearchum.ui

import com.example.biblesearchum.model.BibleBook
import com.example.biblesearchum.model.BibleVerse
import com.example.biblesearchum.network.BibleApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

private val GENESIS = BibleBook(bookId = 1, name = "Génesis", chapters = 50)
private val SALMOS = BibleBook(bookId = 19, name = "Salmos", chapters = 150)

private class FakeBibleApi : BibleApiService {
    var booksError: Exception? = null
    var verseError: Exception? = null

    override suspend fun getBooks(): List<BibleBook> {
        booksError?.let { throw it }
        return listOf(GENESIS, SALMOS)
    }

    override suspend fun getChapter(bookId: Int, chapter: Int): List<BibleVerse> {
        val count = if (bookId == SALMOS.bookId && chapter == 119) 176 else 31
        return (1..count).map { BibleVerse(pk = it, verse = it, text = "v$it") }
    }

    override suspend fun getVerse(bookId: Int, chapter: Int, verse: Int): BibleVerse {
        verseError?.let { throw it }
        return BibleVerse(pk = 1, verse = verse, text = "En el principio<br>creó Dios,  ")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BibleViewModelTest {

    private val api = FakeBibleApi()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga los libros al iniciar`() {
        val state = BibleViewModel(api).uiState.value

        assertEquals(listOf(GENESIS, SALMOS), state.books)
        assertEquals(false, state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `muestra error si fallan los libros y reintentar los recupera`() {
        api.booksError = IOException("sin red")
        val viewModel = BibleViewModel(api)

        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertNotNull(viewModel.uiState.value.errorMessage)

        api.booksError = null
        viewModel.retry()

        assertEquals(2, viewModel.uiState.value.books.size)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `al elegir libro se generan sus capitulos`() {
        val viewModel = BibleViewModel(api)
        viewModel.onBookSelected(GENESIS)

        assertEquals((1..50).toList(), viewModel.uiState.value.availableChapters)
        assertTrue(viewModel.uiState.value.availableVerses.isEmpty())
    }

    @Test
    fun `al elegir capitulo se usa el numero real de versiculos`() {
        val viewModel = BibleViewModel(api)
        viewModel.onBookSelected(SALMOS)
        viewModel.onChapterSelected(119)

        assertEquals((1..176).toList(), viewModel.uiState.value.availableVerses)
    }

    @Test
    fun `al elegir versiculo se muestra su texto limpio`() {
        val viewModel = BibleViewModel(api)
        viewModel.onBookSelected(GENESIS)
        viewModel.onChapterSelected(1)
        viewModel.onVerseSelected(1)

        val state = viewModel.uiState.value
        assertEquals("En el principio creó Dios,", state.currentVerseContent?.text)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `cambiar de libro limpia capitulo, versiculo y texto`() {
        val viewModel = BibleViewModel(api)
        viewModel.onBookSelected(GENESIS)
        viewModel.onChapterSelected(1)
        viewModel.onVerseSelected(1)

        viewModel.onBookSelected(SALMOS)

        val state = viewModel.uiState.value
        assertEquals(SALMOS, state.selectedBook)
        assertNull(state.selectedChapter)
        assertNull(state.selectedVerse)
        assertTrue(state.availableVerses.isEmpty())
        assertNull(state.currentVerseContent)
    }

    @Test
    fun `cambiar de capitulo limpia versiculo y texto`() {
        val viewModel = BibleViewModel(api)
        viewModel.onBookSelected(GENESIS)
        viewModel.onChapterSelected(1)
        viewModel.onVerseSelected(1)

        viewModel.onChapterSelected(2)

        val state = viewModel.uiState.value
        assertEquals(2, state.selectedChapter)
        assertNull(state.selectedVerse)
        assertNull(state.currentVerseContent)
    }

    @Test
    fun `error al obtener versiculo muestra mensaje`() {
        api.verseError = IOException("sin red")
        val viewModel = BibleViewModel(api)
        viewModel.onBookSelected(GENESIS)
        viewModel.onChapterSelected(1)
        viewModel.onVerseSelected(1)

        val state = viewModel.uiState.value
        assertNull(state.currentVerseContent)
        assertNotNull(state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `reiniciar conserva los libros y borra la seleccion`() {
        val viewModel = BibleViewModel(api)
        viewModel.onBookSelected(GENESIS)
        viewModel.onChapterSelected(1)
        viewModel.onVerseSelected(1)

        viewModel.clearSelection()

        assertEquals(BibleUiState(books = listOf(GENESIS, SALMOS)), viewModel.uiState.value)
    }
}
