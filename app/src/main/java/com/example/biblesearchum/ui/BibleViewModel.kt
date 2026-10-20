package com.example.biblesearchum.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblesearchum.model.BibleBook
import com.example.biblesearchum.model.toPlainVerseText
import com.example.biblesearchum.network.BibleApiService
import com.example.biblesearchum.network.RetrofitInstance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * El servicio se recibe por constructor para poder sustituirlo por uno falso en los tests.
 * Como el parámetro tiene valor por defecto, `viewModel()` sigue pudiendo crearlo sin factory.
 */
class BibleViewModel(
    private val api: BibleApiService = RetrofitInstance.api
) : ViewModel() {

    private val _uiState = MutableStateFlow(BibleUiState())
    val uiState: StateFlow<BibleUiState> = _uiState.asStateFlow()

    private var booksJob: Job? = null

    // Petición en curso para la selección actual (versículos del capítulo o texto del versículo).
    // Se cancela al cambiar la selección para que una respuesta lenta no pise a la más reciente.
    private var selectionJob: Job? = null

    init {
        fetchBooks()
    }

    fun fetchBooks() {
        booksJob?.cancel()
        booksJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val bookList = api.getBooks()
                _uiState.update { it.copy(books = bookList, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar libros. ${e.toUserMessage()}"
                    )
                }
            }
        }
    }

    fun onBookSelected(book: BibleBook) {
        if (book == _uiState.value.selectedBook) return

        selectionJob?.cancel()
        val chaptersList = (1..book.chapters).toList()
        _uiState.update {
            it.copy(
                selectedBook = book,
                availableChapters = chaptersList,
                selectedChapter = null,
                selectedVerse = null,
                availableVerses = emptyList(),
                currentVerseContent = null,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun onChapterSelected(chapter: Int) {
        val state = _uiState.value
        val currentBook = state.selectedBook ?: return
        if (chapter == state.selectedChapter && state.availableVerses.isNotEmpty()) return

        selectionJob?.cancel()
        _uiState.update {
            it.copy(
                selectedChapter = chapter,
                availableVerses = emptyList(),
                selectedVerse = null,
                currentVerseContent = null,
                isLoading = true,
                errorMessage = null
            )
        }

        // En lugar de suponer 150 versículos (Salmos 119 tiene 176), se consulta el capítulo real
        selectionJob = viewModelScope.launch {
            try {
                val versesList = api.getChapter(currentBook.bookId, chapter)
                    .map { it.verse }
                    .distinct()
                    .sorted()
                _uiState.update {
                    it.copy(
                        availableVerses = versesList,
                        isLoading = false,
                        errorMessage = if (versesList.isEmpty()) {
                            "${currentBook.name} $chapter no tiene versículos disponibles."
                        } else {
                            null
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudieron cargar los versículos de " +
                            "${currentBook.name} $chapter. ${e.toUserMessage()}"
                    )
                }
            }
        }
    }

    fun onVerseSelected(verse: Int) {
        val currentBook = _uiState.value.selectedBook ?: return
        val currentChapter = _uiState.value.selectedChapter ?: return

        selectionJob?.cancel()
        _uiState.update {
            it.copy(
                selectedVerse = verse,
                currentVerseContent = null,
                isLoading = true,
                errorMessage = null
            )
        }

        selectionJob = viewModelScope.launch {
            try {
                val result = api.getVerse(currentBook.bookId, currentChapter, verse)
                _uiState.update {
                    it.copy(
                        currentVerseContent = result.copy(text = result.text.toPlainVerseText()),
                        isLoading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No se pudo obtener el versículo solicitado. ${e.toUserMessage()}"
                    )
                }
            }
        }
    }

    /** Repite la última operación que falló. */
    fun retry() {
        val state = _uiState.value
        when {
            state.books.isEmpty() -> fetchBooks()
            state.selectedVerse != null -> onVerseSelected(state.selectedVerse)
            state.selectedChapter != null -> onChapterSelected(state.selectedChapter)
        }
    }

    fun clearSelection() {
        selectionJob?.cancel()
        _uiState.update { BibleUiState(books = it.books, isLoading = booksJob?.isActive == true) }
        if (_uiState.value.books.isEmpty() && booksJob?.isActive != true) fetchBooks()
    }

    private fun Exception.toUserMessage(): String = when (this) {
        is IOException -> "Revisa tu conexión a Internet e inténtalo de nuevo."
        is HttpException -> "El servidor respondió con el código ${code()}."
        else -> "Ocurrió un error inesperado."
    }
}
