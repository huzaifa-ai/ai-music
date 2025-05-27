package com.holisticapp.fitness.api

import retrofit2.Response
import retrofit2.http.*

interface SpotifyApiService {
    
    @FormUrlEncoded
    @POST("api/token")
    suspend fun getAccessToken(
        @Header("Authorization") authorization: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): Response<SpotifyTokenResponse>
    
    @GET("v1/search")
    suspend fun searchTracks(
        @Header("Authorization") authorization: String,
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("limit") limit: Int = 20,
        @Query("market") market: String = "US"
    ): Response<SpotifySearchResponse>
}

data class SpotifyTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)

data class SpotifySearchResponse(
    val tracks: SpotifyTracks
)

data class SpotifyTracks(
    val items: List<SpotifyTrack>
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtist>,
    val album: SpotifyAlbum,
    val duration_ms: Int,
    val preview_url: String?,
    val external_urls: SpotifyExternalUrls
)

data class SpotifyArtist(
    val name: String
)

data class SpotifyAlbum(
    val name: String,
    val images: List<SpotifyImage>
)

data class SpotifyImage(
    val url: String,
    val height: Int,
    val width: Int
)

data class SpotifyExternalUrls(
    val spotify: String
) 