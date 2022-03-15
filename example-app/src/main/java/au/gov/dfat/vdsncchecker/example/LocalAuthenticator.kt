package au.gov.dfat.vdsncchecker.example

import au.gov.dfat.lib.vdsncchecker.VDSAuthenticator
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.security.cert.CertificateFactory

class LocalAuthenticator : VDSAuthenticator(){


    override fun setupSecurity() {
        if(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null){
            Security.addProvider(BouncyCastleProvider())
        }
    }

    override fun onGetCertificateFactory(): CertificateFactory {
        try{
            var factory = CertificateFactory.getInstance("X.509", BouncyCastleProvider())
            return factory
        }
        catch(ex: java.lang.Exception){

        }
        throw java.lang.Exception("failed")


    }



}