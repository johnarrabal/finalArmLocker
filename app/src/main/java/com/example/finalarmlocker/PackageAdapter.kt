package com.example.finalarmlocker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finalarmlocker.MqttHandler
import com.example.finalarmlocker.R
import com.example.projetoarmario.FirebaseDatabaseHelper

class PackageAdapter(
    private val firebaseDatabaseHelper: FirebaseDatabaseHelper,
    private val userCpf: String
) : RecyclerView.Adapter<PackageAdapter.PackageViewHolder>() {

    private var packages: List<FirebaseDatabaseHelper.Package> = listOf()

    // Define a lista de pacotes e notifica o adaptador sobre a mudança de dados
    fun setPackages(packages: List<FirebaseDatabaseHelper.Package>) {
        this.packages = packages
        notifyDataSetChanged()
    }

    // Infla o layout do item de pacote e cria um PackageViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_package, parent, false)
        return PackageViewHolder(view)
    }

    // Liga os dados do pacote ao PackageViewHolder
    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        val pkg = packages[position]
        holder.bind(pkg)
    }

    // Retorna o número de pacotes na lista
    override fun getItemCount(): Int = packages.size

    // Classe interna para gerenciar a exibição de um item de pacote
    inner class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textApartmentNumber: TextView = itemView.findViewById(R.id.textApartmentNumber)
        private val textCPF: TextView = itemView.findViewById(R.id.textCPF)
        private val buttonDeletePackage: Button = itemView.findViewById(R.id.buttonDeletePackage)

        // Liga os dados do pacote aos elementos de UI
        fun bind(pkg: FirebaseDatabaseHelper.Package) {
            textApartmentNumber.text = pkg.apartmentNumber
            textCPF.text = pkg.cpf ?: "CPF not available"

            // Configura o botão de exclusão para deletar o pacote e enviar uma mensagem MQTT
            buttonDeletePackage.setOnClickListener {
                firebaseDatabaseHelper.deletePackageById(pkg.id.toString()) { success ->
                    if (success) {
                        pkg.cpf?.let { cpf ->
                            sendMqttMessage(cpf, "abrir", "cliente")
                        }
                        packages = packages.filter { it.id != pkg.id }
                        notifyDataSetChanged()
                    }
                }
            }
        }

        // Conecta ao broker MQTT, publica uma mensagem e desconecta
        private fun sendMqttMessage(cpf: String, acao: String, tipo: String) {
            val mqttHandler = MqttHandler()
            val brokerUrl = "tcp://broker.emqx.io:1883"
            val clientId = "Armario"
            val username = "Armario"
            val password = "1235"
            mqttHandler.connect(brokerUrl, clientId, username, password)
            val message = """{"cpf": "$cpf", "acao": "$acao", "tipo": "$tipo"}"""
            mqttHandler.publish("api/saida", message)
            mqttHandler.disconnect()
        }
    }
}