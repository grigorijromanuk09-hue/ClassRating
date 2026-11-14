package ru.classrating.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

// ВАЖНО: пути и поля ниже соответствуют публичному API Дневник.ру v2, но их нужно
// сверить с документацией, которую вы получите при регистрации приложения.
data class IdDto(val id: String?)
data class ContextDto(val personId: String?, val schools: List<IdDto>?, val eduGroups: List<IdDto>?)
data class SubjectDto(val id: String?, val name: String?)
data class LessonDto(val id: String?, val date: String?, val number: Int?, val subject: SubjectDto?, val place: String?)
data class MarkDto(val id: String?, val date: String?, val value: String?, val lesson: String?)

interface DnevnikApi {
    @GET("users/me/context")
    suspend fun context(@Header("Access-Token") token: String): ContextDto

    @GET("edu-groups/{group}/lessons/{from}/{to}")
    suspend fun lessons(
        @Header("Access-Token") token: String,
        @Path("group") group: String, @Path("from") from: String, @Path("to") to: String
    ): List<LessonDto>

    @GET("persons/{person}/groups/{group}/marks/{from}/{to}")
    suspend fun marks(
        @Header("Access-Token") token: String,
        @Path("person") person: String, @Path("group") group: String,
        @Path("from") from: String, @Path("to") to: String
    ): List<MarkDto>
}
