package com.example.finalarmlocker

// Classe de dados para representar a resposta de um serviço de busca de endereços
data class AddressResponse(
    val cep: String,          // Código postal
    val logradouro: String,   // Nome da rua
    val complemento: String,  // Complemento do endereço
    val bairro: String,       // Bairro
    val localidade: String,   // Cidade
    val uf: String,           // Unidade Federativa (estado)
    val unidade: String,      // Unidade (geralmente não utilizado)
    val ibge: String,         // Código IBGE da localidade
    val gia: String           // Código GIA (geralmente não utilizado)
)