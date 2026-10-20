package com.example.biblesearchum.network

import com.example.biblesearchum.model.BibleBook
import com.example.biblesearchum.model.BibleVerse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Código de la Reina-Valera 1960 en Bolls Bible.
 * Ojo: la API usa "RV1960"; con "RVR1960" responde "There is no such translation".
 */
const val TRANSLATION = "RV1960"
const val TRANSLATION_NAME = "Reina-Valera 1960"

interface BibleApiService {
    @GET("get-books/$TRANSLATION/")
    suspend fun getBooks(): List<BibleBook>

    /** Capítulo completo: se usa para saber cuántos versículos tiene realmente. */
    @GET("get-text/$TRANSLATION/{bookId}/{chapter}/")
    suspend fun getChapter(
        @Path("bookId") bookId: Int,
        @Path("chapter") chapter: Int
    ): List<BibleVerse>

    @GET("get-verse/$TRANSLATION/{bookId}/{chapter}/{verse}/")
    suspend fun getVerse(
        @Path("bookId") bookId: Int,
        @Path("chapter") chapter: Int,
        @Path("verse") verse: Int
    ): BibleVerse
}

object RetrofitInstance {
    private const val BASE_URL = "https://bolls.life/"

    val api: BibleApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BibleApiService::class.java)
    }
}
