package com.example.finalarmlocker

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

/**
 * Classe Application do app (registrada no AndroidManifest).
 *
 * O cache offline do Firebase precisa ser ligado uma única vez, antes de qualquer uso do banco.
 * Antes ele era ligado na MainActivity, o que derrubava o app quando ela era recriada
 * (por exemplo, ao sair da conta).
 */
class ArmLockerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}
