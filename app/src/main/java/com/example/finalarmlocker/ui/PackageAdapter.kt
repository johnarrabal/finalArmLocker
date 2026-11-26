package com.example.finalarmlocker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.finalarmlocker.R
import com.example.finalarmlocker.data.LockerPackage
import com.example.finalarmlocker.databinding.ItemPackageBinding

/**
 * Lista de encomendas do morador. O adapter só desenha os itens; o que acontece ao tocar
 * em "Retirar" fica na tela (callback [onWithdrawClick]).
 */
class PackageAdapter(
    private val onWithdrawClick: (LockerPackage) -> Unit,
) : ListAdapter<LockerPackage, PackageAdapter.PackageViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = ItemPackageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PackageViewHolder(private val binding: ItemPackageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pkg: LockerPackage) {
            val context = binding.root.context
            binding.textLocker.text = pkg.locker ?: context.getString(R.string.locker_unknown)
            binding.textApartmentNumber.text =
                context.getString(R.string.package_apartment, pkg.apartmentNumber.orEmpty())
            binding.buttonWithdraw.setOnClickListener { onWithdrawClick(pkg) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<LockerPackage>() {
            override fun areItemsTheSame(oldItem: LockerPackage, newItem: LockerPackage) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: LockerPackage, newItem: LockerPackage) = oldItem == newItem
        }
    }
}
