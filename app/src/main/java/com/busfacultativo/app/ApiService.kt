package com.busfacultativo.app

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
interface ApiService {

    @GET("api/sync/balances")
    fun obtenerBalances(): Call<BalancesResponse>
    @POST("api/register")
    fun registrarUsuario(
        @Body request: RegisterRequest
    ): Call<RegisterResponse>
    @POST("api/login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>
    @POST("api/recargar")
    fun recargar(
        @Body request: RecargaRequest
    ): Call<RecargaResponse>

    @POST("api/generar-qr")
    fun generarQR(
        @Body request: QRRequest
    ): Call<QRResponse>

    @POST("api/verificar-pago")
    fun verificarPago(
        @Body request: VerificarPagoRequest
    ): Call<VerificarPagoResponse>

    @GET("api/usuario/{ru}")
    fun obtenerUsuario(
        @Path("ru") ru: String
    ): Call<UsuarioResponse>

    @GET("api/historial/{ru}")
    fun obtenerHistorial(
        @Path("ru") ru: String
    ): Call<List<HistorialItem>>
}