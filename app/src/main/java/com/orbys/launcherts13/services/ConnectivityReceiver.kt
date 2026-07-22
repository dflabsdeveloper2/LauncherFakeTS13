package com.orbys.launcherts13.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.os.storage.StorageManager
import android.util.Log

/**
 * Receiver to detect changes in Wi-Fi, Ethernet, Hotspot, and USB Storage connectivity.
 */
class ConnectivityReceiver(private val callback: (ConnectivityStatus) -> Unit) : BroadcastReceiver() {

    data class ConnectivityStatus(
        val isWifiConnected: Boolean,
        val isEthernetConnected: Boolean,
        val isHotspotEnabled: Boolean,
        val isUsbConnected: Boolean,
        val isBluetoothEnabled: Boolean
    )

    override fun onReceive(context: Context, intent: Intent) {
        val status = getConnectivityStatus(context)
        callback(status)
    }

    /**
     * Checks current connectivity status for all tracked types.
     */
    fun getConnectivityStatus(context: Context): ConnectivityStatus {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        var wifi = false
        var ethernet = false
        
        val nw = cm.activeNetwork
        if (nw != null) {
            val actNw = cm.getNetworkCapabilities(nw)
            if (actNw != null) {
                wifi = actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                ethernet = actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            }
        }

        // Hotspot status (WIFI_AP_STATE_ENABLED = 13)
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val isHotspot = try {
            val method = wm.javaClass.getDeclaredMethod("getWifiApState")
            val state = method.invoke(wm) as Int
            state == 13 || state == 12 // 13: Enabled, 12: Enabling
        } catch (e: Exception) {
            false
        }

        // USB Status (Storage)
        val isUsb = checkUsbConnected(context)

        // Bluetooth Status
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val isBluetooth = btAdapter?.isEnabled ?: false

        return ConnectivityStatus(wifi, ethernet, isHotspot, isUsb, isBluetooth)
    }

    private fun checkUsbConnected(context: Context): Boolean {
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        return try {
            sm.storageVolumes.any { it.isRemovable && it.state == android.os.Environment.MEDIA_MOUNTED }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        /**
         * Returns an IntentFilter for general connectivity changes.
         * Media actions often require a separate filter with a data scheme.
         */
        fun getGeneralFilter(): IntentFilter {
            return IntentFilter().apply {
                @Suppress("DEPRECATION")
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
        }

        /**
         * Returns an IntentFilter for media/USB changes.
         */
        fun getMediaFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(Intent.ACTION_MEDIA_MOUNTED)
                addAction(Intent.ACTION_MEDIA_REMOVED)
                addAction(Intent.ACTION_MEDIA_UNMOUNTED)
                addDataScheme("file")
            }
        }
    }
}
