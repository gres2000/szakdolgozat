package com.example.myapplication.app.main_activity.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

class InternetConnectivityCallback(private val callback: (Boolean) -> Unit) :
    ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        callback(true)
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        callback(false)
    }

    companion object {
        fun registerConnectivityCallback(context: Context, callback: (Boolean) -> Unit): InternetConnectivityCallback {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val connectivityCallback = InternetConnectivityCallback(callback)
            val networkRequest = android.net.NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, connectivityCallback)
            return connectivityCallback
        }

        fun unregisterConnectivityCallback(context: Context, connectivityCallback: InternetConnectivityCallback) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(connectivityCallback)
        }
    }
}

