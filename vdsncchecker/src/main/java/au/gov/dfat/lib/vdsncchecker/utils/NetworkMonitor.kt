package au.gov.dfat.lib.vdsncchecker.utils
//  Copyright (c) 2021, Commonwealth of Australia. vds.support@dfat.gov.au
//
//  Licensed under the Apache License, Version 2.0 (the "License"); you may not
//  use this file except in compliance with the License. You may obtain a copy
//  of the License at:
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
//  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
//  License for the specific language governing permissions and limitations
//  under the License.

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.CountDownTimer
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference

/**
 *
 * class for checking network connectivity
 *
 */
open class NetworkMonitor protected constructor() {

    companion object{
        private var INSTANCE: NetworkMonitor? = null

        @RequiresApi(Build.VERSION_CODES.N)
        fun getInstance(context: Context, logger:Logger? = null): NetworkMonitor{
            if(INSTANCE == null){

                val monitor = NetworkMonitor()
                monitor.logger = logger
                monitor.setupMonitor(context.applicationContext)
                logger?.printLine("Set up network monitor")
                INSTANCE = monitor

                return monitor
            }

            return INSTANCE!!

        }
    }

    var logger: Logger? = null

    // is connectivity available
    private var _available: Boolean = false
    val available: Boolean get() {
        return _available
    }

    // minute timer for showing warnings
    var timer: CountDownTimer = object: CountDownTimer(60 * 1000, 1000){

        override fun onFinish() {

            networkListeners.forEach { listener ->
                listener?.get()?.networkState(NetworkState.Lost)
            }

            this.cancel()
        }

        override fun onTick(p0: Long) {
            // do nothing
            
        }

    }

    @RequiresApi(Build.VERSION_CODES.N)
    /**
     *
     * Setup the monitor's behaviour. Called once on Instance creation
     *
     * @param context - context ( from which application context is derived )
     *
     */
    private fun setupMonitor(context: Context)
    {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val capabilities =  cm.getNetworkCapabilities(cm.activeNetwork)

        if(capabilities != null){

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            )
            {
                _available = true
                logger?.printLine("Network is available")
            }
        } else{
            logger?.printLine("Network capabilities are null")
        }
        // register callbacks on network connectivity changes
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                timer.cancel()
                _available = true
                logger?.printLine("Notifying ${networkListeners.size} listeners that network has connected")
                networkListeners.forEach { listener ->
                    listener.get()?.networkState(NetworkState.Connected)
                }
            }
            override fun onLost(network: Network) {
                super.onLost(network)
                timer.start()
                _available = false
                logger?.printLine("Notifying ${networkListeners.size} listeners that network has dropped")
                networkListeners.forEach { listener ->
                    listener.get()?.networkState(NetworkState.Dropped)
                }
            }
        })
    }

    /**
     *
     * add network listener to list of listeners
     *
     * @param listener - the CertificateRepositoryNetworkDelegate to listen for network changes
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun setNetworkListener(listener: NetworkDelegate) {
        val ref = WeakReference(listener)
        if (!networkListeners.contains(ref)) {
            networkListeners.add(ref)
        }
        networkListeners.removeIf { l -> l.get() == null }
    }

    /**
     *
     * remove a listener from the list
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun removeNetworkListener(listener: NetworkDelegate) {
        networkListeners.removeIf { l -> l.get() == listener }
    }

    /**
     *
     * listeners for network connectivity state updates
     *
     */
    private val networkListeners: MutableList<WeakReference<NetworkDelegate>> =
        mutableListOf()

    fun interface NetworkDelegate{
        fun networkState(state: NetworkState)
    }

}

/**
 *
 * enum for defining the state of the network
 *
 */
enum class NetworkState{
    Connected,
    Dropped,
    Lost
}