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

package au.gov.dfat.lib.vdsncchecker.data

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.RequiresApi
import au.gov.dfat.lib.vdsncchecker.VaultError
import au.gov.dfat.lib.vdsncchecker.VaultException
import java.io.*
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyStore
import java.security.cert.Certificate
import java.util.*
import javax.crypto.spec.SecretKeySpec

/**
 *
 * Secure Vault implementation to store more sensitive data
 *
 */
@RequiresApi(Build.VERSION_CODES.O)
class CertificateVault private constructor(private val homeDir: String) : VaultBase(
    Paths.get(homeDir, ".secrets", "key.store"),
    Paths.get(homeDir, ".secrets", "key.password")) {

    /**
     *
     * override of abstract method
     *
     */
    override fun entryReturn(entry: KeyStore.Entry): String {
        if(entry is KeyStore.TrustedCertificateEntry){
            return certificateReturn(entry)
        }
        return String((entry as KeyStore.SecretKeyEntry).secretKey.encoded)
    }

    /**
     *
     * method to return string representation of certificate
     *
     */
    private fun certificateReturn(entry: KeyStore.Entry): String{
        return String((entry as KeyStore.TrustedCertificateEntry).trustedCertificate.encoded)
    }

    companion object{

        private var appContext: Context? = null

        fun setContext(app: Context)
        {
            appContext = app
        }

        fun getInstance(context: Context? = null): CertificateVault?{

            if(context == null && appContext == null)
                throw Exception("Application context must first be initialised via a call to Vault.setContext or passed into getInstance")

            if(context != null){
                appContext = context
            }

            if(instance == null){
                val wrapper = ContextWrapper(appContext).baseContext.dataDir.absolutePath
                instance = CertificateVault(wrapper)
            }

            return instance


        }

        private var instance: CertificateVault? = null
    }


}

/**
 *
 * base abstract secure vault class
 * used to store sensitive data, such as certificates
 *
 * @param keyStorePath - path of keystore
 * @param passwordPath - path of password file
 *
 */
@RequiresApi(Build.VERSION_CODES.O)
abstract class VaultBase(
    private val keyStorePath: Path = Paths.get(System.getProperty("user.home"), ".secrets", "key.store"),
                     private val passwordPath: Path = Paths.get(System.getProperty("user.home"), ".secrets", "key.password")
              ) {


    private val keyStoreFile: File
        get() {
            try {
                val dirPath = keyStorePath.parent

                if (!dirPath.toFile().exists()) {
                    dirPath.toFile().mkdirs()
                }

                return keyStorePath.toFile()
            }
            catch(e: Exception)
            {
                throw VaultException(VaultError.GET_STORE_FILE)
            }
        }


    private val keyStore: KeyStore by lazy {
        try{
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

            if (keyStoreFile.exists()) {
                val stream = FileInputStream(keyStoreFile)
                keyStore.load(stream, password)
            } else {
                keyStore.load(null, password)
            }

            keyStore
        }
        catch(exception: IOException){
            throw VaultException(VaultError.LOAD_KEYSTORE)
        }
    }

    private val password: CharArray by lazy {

        val dirPath = passwordPath.parent

        if (!dirPath.toFile().exists()) {
            dirPath.toFile().mkdirs()
        }

        val path = passwordPath.toFile()

        if (path.exists()) {
            path.readText().toCharArray()
        } else {
            val uuid = UUID.randomUUID().toString()
            path.writeText(uuid)
            uuid.toCharArray()
        }
    }

    /**
     *
     * get entry from keystore
     *
     * @param entry - key identifying keystore entry
     *
     * @return string representation of data
     *
     */
    @Throws(VaultException::class)
    fun getEntry(entry: String): String? {
        try{
            if (!keyStoreFile.exists()) {
                return null
            }

            val protection = KeyStore.PasswordProtection(password)
            val secret = keyStore.getEntry(entry, protection)

            return if (secret != null) {
                entryReturn(secret)
            } else {
                return ""
            }
        }
        catch(exception: Exception){
            throw VaultException(VaultError.GET_ENTRY)
        }
    }

    /**
     *
     * abstract method to process data retrieved from keystore
     *
     * @param entry - data from keystore
     *
     * @return String representation of keystore data
     *
     */
    abstract fun entryReturn(entry: KeyStore.Entry): String

    /**
     *
     * insert certificate into keystore
     *
     * @param entry - key to identify data being inserted
     * @param value - certificate data to be stored
     *
     */
    @Throws(VaultException::class)
    fun setCertificateEntry(entry: String, value: Certificate){
        try {
            val protection = KeyStore.PasswordProtection(password)
            keyStore.setEntry(entry, KeyStore.TrustedCertificateEntry(value), protection)
            storeEntry()
        }
        catch(e: Exception)
        {
            throw VaultException(VaultError.SET_ENTRY)
        }
    }

    /**
     *
     * insert secret key entry into keystore
     *
     * @param entry - key to identify data being stored
     * @param value - string value being stored
     *
     * @throws VaultException - if error occurs while trying to store entry
     *
     */
    @Throws(VaultException::class)
    fun setEntry(entry: String, value: String) {
        if(value == "")
            return

        try {
            val protection = KeyStore.PasswordProtection(password)

            val encoded = SecretKeySpec(value.toByteArray(), "AES")

            keyStore.setEntry(entry, KeyStore.SecretKeyEntry(encoded), protection)

            storeEntry()
        }
        catch(e: Exception)
        {
            throw VaultException(VaultError.SET_ENTRY)
        }
    }

    /**
     *
     * saves store file. Called after insert
     *
     */
    @Throws(VaultException::class)
    private fun storeEntry(){
        try {
            val stream = FileOutputStream(keyStoreFile)
            stream.use {
                keyStore.store(it, password)
            }
        }
        catch (e: Exception)
        {
            throw VaultException(VaultError.STORE_ENTRY)
        }
    }

    fun destroy() {
        Files.deleteIfExists(keyStorePath)
        Files.deleteIfExists(passwordPath)
    }
}

