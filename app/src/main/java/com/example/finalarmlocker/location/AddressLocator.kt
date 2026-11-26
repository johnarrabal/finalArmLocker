package com.example.finalarmlocker.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Descobre o endereço onde o aparelho está, usando uma única leitura de localização.
 *
 * Antes o app pedia atualizações a cada 5 segundos e nunca as cancelava no cadastro,
 * gastando bateria enquanto a tela estivesse aberta. A conversão de coordenadas em
 * endereço (Geocoder) acessa a rede, então roda fora da thread principal.
 */
class AddressLocator(context: Context) {

    data class DeviceAddress(
        val cep: String?,
        val street: String?,
        val city: String?,
        val state: String?,
    )

    private val appContext = context.applicationContext
    private val locationClient = LocationServices.getFusedLocationProviderClient(appContext)

    fun hasPermission(): Boolean = PERMISSIONS.any {
        ContextCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Endereço atual, ou `null` se não houver permissão, GPS ou Geocoder disponível. */
    suspend fun currentAddress(): DeviceAddress? {
        if (!hasPermission()) return null
        val location = currentLocation() ?: return null
        return withContext(Dispatchers.IO) { geocode(location) }
    }

    @SuppressLint("MissingPermission") // a permissão é conferida em currentAddress()
    private suspend fun currentLocation(): Location? {
        val cancellation = CancellationTokenSource()
        return try {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token).await()
            } ?: locationClient.lastLocation.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Localização indisponível", e)
            null
        } finally {
            cancellation.cancel()
        }
    }

    @Suppress("DEPRECATION") // a versão assíncrona do Geocoder só existe a partir do Android 13
    private fun geocode(location: Location): DeviceAddress? {
        if (!Geocoder.isPresent()) return null
        return try {
            Geocoder(appContext, Locale("pt", "BR"))
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
                ?.let {
                    DeviceAddress(
                        cep = it.postalCode,
                        street = it.thoroughfare,
                        city = it.locality ?: it.subAdminArea,
                        state = it.adminArea,
                    )
                }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder falhou", e)
            null
        }
    }

    companion object {
        private const val TAG = "AddressLocator"
        private const val LOCATION_TIMEOUT_MS = 10_000L

        /** No Android 12+ é preciso pedir as duas; o usuário pode conceder só a aproximada. */
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
