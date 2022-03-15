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

package au.gov.dfat.lib.vdsncchecker.security

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import java.io.File
import android.os.Build
import android.os.Debug
import au.gov.dfat.lib.vdsncchecker.R
import java.io.IOException
import java.lang.Exception
import java.lang.NumberFormatException
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


private const val su:String = "su"
private const val busyBox:String = "busybox"
private const val maxServices:Int = 300


/**
 *
 * class for managing security functions, such as Root detection
 *
 */
open class SecurityManager(private val context: Context? = null) {

    companion object{
        init{
            // load cpp library
            System.loadLibrary("vdsncchecker")
        }
    }


    /**
     *
     * run external checks ( cpp )
     *
     * @return int
     *
     */
    fun runExternalTests(): HashMap<String, Int>{

        val antiDebug = "antiDebug"
        val pathCheck = "pathCheck"

        // set results to failure
        var results = hashMapOf(
            Pair(antiDebug, -1),
            Pair(pathCheck, -1)
        )

        try{
            antiDebug()

            results[pathCheck] = pathCheck()
            results[antiDebug] = 1

            return results
        }
        catch(_: Exception)
        {}
        // return failure results if this is reached
        return results


    }

    /**
     *
     * run all non-external debug checks
     *
     * @return HashMap of results, named after the methods that are called
     *
     */
    fun runAllChecks(): HashMap<String, Any>{

        // result container
        var results = hashMapOf<String, Any>()
        results["runningProcesses"] = checkRunningProcesses()
        results["checkRoot"] = checkRoot()
        results["isTestKeyBuild"] = isTestKeyBuild()
        results["isDebuggerConnected"] = isDebuggerConnected()

        if(context != null){
            results["checkInstalledApps"] = checkInstalledApps().any()
            results["isDebuggable"] = isDebuggable()

            try{
                val crcString = context.getString(R.string.dex_crc)
                if(!crcString.isNullOrEmpty()) {
                    val crcLong = crcString.toLong()
                    results["crcCheck"] = crcIsModified(crcLong)
                }
            }
            catch(_: NumberFormatException){
                // not valid CRC
            }
            catch(_: Exception){

            }

        }
        return results
    }


    /**
     *
     * detect slow down due to debugger being connected
     *
     */
    fun detect_threadCpuTimeNanos(): Boolean {
        val start = Debug.threadCpuTimeNanos()
        for (i in 0..999999) continue
        val stop = Debug.threadCpuTimeNanos()
        return stop - start >= 10000000
    }


    /**
     *
     * super user names to look out for
     *
     */
    protected open val superUsers:ArrayList<String> = arrayListOf("supersu", "superuser")


    /**
     *
     * directories to check for
     *
     */
    protected open val locations:ArrayList<String> =
        arrayListOf(
            "/sbin/",
            "/system/bin/",
            "/system/bin/failsafe/",
            "/system/xbin/",
            "/system/sd/xbin/",
            "/data/local/",
            "/data/local/xbin/",
            "/data/local/bin/"
        )

    /**
     *
     * words to look out for in nefarious packages
     *
     */
    protected open val packageWords:ArrayList<String> =
        arrayListOf(
            "chainfire",
            "noshufou",
            "koushikdutta",
            "zachspong",
            "temprootremovejb",
            "ramdroid",
            "appquarantine",
            "topjohnwu",
            "magisk",
            "thirdparty",
            "busybox"
        )

    /**
     *
     * check running processes for anything run by super users
     *
     * @return boolean representing if su is running anything
     *
     */
    fun checkRunningProcesses():Boolean{

        val manager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            ?: // cannot check, no context
            return false

        // Get currently running application processes
        @SuppressWarnings("deprecation")
        val list: List<ActivityManager.RunningServiceInfo> = manager.getRunningServices(maxServices)
        if (list.count() > 0) {
            val tempName: String
            for (i in list.indices) {
                tempName = list[i].process

                return superUsers.contains(tempName)
            }
        }
        return false
    }


    /**
     *
     * check if root su directory exists
     *
     * @return boolean representing if a 'su' labelled directory exists
     *
     */
    fun checkRoot(): Boolean{
        val dir = System.getenv("PATH")
        if(dir != null){
            for (pathDir in dir.split(":").toTypedArray()) {
                if (File(pathDir, su).exists()) {
                    return true
                }
            }
        }

        return false
    }


    /**
     *
     * check for test-keys flag
     *
     * @return boolean representing if build tags contain test-keys
     *
     */
    private fun isTestKeyBuild(): Boolean {
        val str = Build.TAGS
        return str != null && str.contains("test-keys")
    }

    /**
     *
     * check packages for any known evil known names
     *
     * @return ArrayList of found apps installed that match any of the listed words to check for
     *
     */
    private fun checkInstalledApps(): ArrayList<String>{
        var foundList:ArrayList<String> = arrayListOf()
        var packageManager = context?.packageManager
        if(packageManager == null){
            // cannot use, no context
            return arrayListOf()
        }
        val list = packageManager.getInstalledPackages(0)
        for(i in list.indices){
            var info = list[i]
            if(info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0){
                val name = info.applicationInfo.loadLabel(packageManager).toString()
                packageWords.forEach {
                    if(name.contains(it)){
                        foundList.add(name)
                    }
                }
            }
        }
        return foundList
    }

    /**
     *
     * checks application flags if debugging is allowed
     *
     * @return boolean representing if can debug
     *
     */
    private fun isDebuggable(): Boolean{
        if(context != null){
            return ((context.applicationContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
        }

        return false
    }

    /**
     *
     * check if Java debugger is connected
     *
     * @return boolean representing if debugger is connected
     *
     */
    private fun isDebuggerConnected(): Boolean{
        return Debug.isDebuggerConnected()
    }

    /**
     *
     * check crc string based on the crc of the classes.dex file
     *
     * @param dexCrc - the crc of the classes.dex file
     *
     * @return boolean representing whether the classes.dex is modified or not
     *
     */
    @Throws(IOException::class)
    private fun crcIsModified(dexCrc: Long): Boolean {

        if(context == null){
            return false;
        }

        var modified = false
        // required dex crc value stored as a text string.
        // it could be any invisible layout element
        val zf = ZipFile(context.packageCodePath)
        val ze: ZipEntry = zf.getEntry("classes.dex")
        modified = ze.crc !== dexCrc

        return modified
    }

    /**
     *
     * kill any anti debug monitor thread that currently exists
     *
     * !! should be called when an activity pauses/destroys !!
     *
     */
    fun killExistingAntiDebug(){
        killAntiDebug()
    }

    // below are references to cpp functions

    @JvmName("ad")
    private external fun antiDebug(logOut: Boolean = isDebuggable())

    @JvmName("b")
    private external fun pathCheck(): Int

    @JvmName("killanti")
    private external fun killAntiDebug()



}