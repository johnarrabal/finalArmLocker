package com.example.finalarmlocker.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/** Consulta de endereço pelo CEP na API pública ViaCEP (https://viacep.com.br). */
interface ViaCepApi {

    /** [cep] deve ter só os 8 dígitos. */
    @GET("{cep}/json/")
    suspend fun getAddress(@Path("cep") cep: String): AddressResponse

    companion object {
        val service: ViaCepApi by lazy {
            Retrofit.Builder()
                .baseUrl("https://viacep.com.br/ws/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ViaCepApi::class.java)
        }
    }
}

/**
 * Resposta da ViaCEP. Para um CEP que não existe, a API responde só `{"erro": true}`,
 * por isso todos os campos podem ser nulos.
 */
data class AddressResponse(
    val cep: String? = null,
    val logradouro: String? = null,
    val complemento: String? = null,
    val bairro: String? = null,
    val localidade: String? = null,
    val uf: String? = null,
    val ibge: String? = null,
    val erro: Boolean? = null,
)
