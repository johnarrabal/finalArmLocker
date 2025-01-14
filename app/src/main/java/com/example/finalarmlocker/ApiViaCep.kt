package com.example.finalarmlocker

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiViaCep {
    @GET("{cep}/json/")
    fun getAddress(@Path("cep") cep: String): Call<AddressResponse>
}
