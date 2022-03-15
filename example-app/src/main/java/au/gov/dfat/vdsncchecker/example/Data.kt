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

package au.gov.dfat.vdsncchecker.example

import android.util.Base64

class Data {
    companion object {

        val JAPAN_CRL = Base64.decode("MIIBSjCB0AIBATAMBggqhkjOPQQDAwUAMG4xCzAJBgNVBAYTAkpQMRwwGgYDVQQK\n" +
                "DBNKYXBhbmVzZSBHb3Zlcm5tZW50MSgwJgYDVQQLDB9UaGUgTWluaXN0cnkgb2Yg\n" +
                "Rm9yZWlnbiBBZmZhaXJzMRcwFQYDVQQDDA5lLXBhc3Nwb3J0Q1NDQRcNMjEwODI1\n" +
                "MDIwMTI0WhcNMjExMTIzMDAwMDAwWqAvMC0wHwYDVR0jBBgwFoAUarTykoK9lkf2\n" +
                "/yoC95RNdJ6XhGMwCgYDVR0UBAMCAUwwDAYIKoZIzj0EAwMFAANnADBkAjAsNP6c\n" +
                "5R8UJhBSJu02debJ3ha0v8x2zzx71zXwmnG8c6N5ilxGAgSpxkcgd1KoJmMCMBxR\n" +
                "I9xdN2iN9hb0I/z1yhj+tlWKrHAxIooyAWh8GwJomFpLDfv/nP3gH3AHu2A3rw==\n", 0)

        val JAPAN_CSCA = Base64.decode("MIIEqDCCBCugAwIBAgICAOMwDAYIKoZIzj0EAwMFADBuMQswCQYDVQQGEwJKUDEc\n" +
                "MBoGA1UECgwTSmFwYW5lc2UgR292ZXJubWVudDEoMCYGA1UECwwfVGhlIE1pbmlz\n" +
                "dHJ5IG9mIEZvcmVpZ24gQWZmYWlyczEXMBUGA1UEAwwOZS1wYXNzcG9ydENTQ0Ew\n" +
                "HhcNMTkwNjE5MDUxNTE5WhcNMzUwNjE5MDUxNTE5WjBuMQswCQYDVQQGEwJKUDEc\n" +
                "MBoGA1UECgwTSmFwYW5lc2UgR292ZXJubWVudDEoMCYGA1UECwwfVGhlIE1pbmlz\n" +
                "dHJ5IG9mIEZvcmVpZ24gQWZmYWlyczEXMBUGA1UEAwwOZS1wYXNzcG9ydENTQ0Ew\n" +
                "ggG1MIIBTQYHKoZIzj0CATCCAUACAQEwPAYHKoZIzj0BAQIxAP//////////////\n" +
                "///////////////////////////+/////wAAAAAAAAAA/////zBkBDD/////////\n" +
                "/////////////////////////////////v////8AAAAAAAAAAP////wEMLMxL6fi\n" +
                "PufkmI4Fa+P4LRkYHZxu/oFBEgMUCI9QE4daxlY5jYou0Z0qhcjt0+wq7wRhBKqH\n" +
                "yiK+iwU3jrHHHvMgrXRuHTtii6ebmFn3QeCCVCo4VQLyXb9VKWw6VF44cnYKtzYX\n" +
                "3kqWJixvXZ6Yv5KS3Cn49B29KJoUfOnaMRO18LjACmCxzh1+gZ16Qx18kOoOXwIx\n" +
                "AP///////////////////////////////8djTYH0Ny3fWBoNskiwp3rs7BlqzMUp\n" +
                "cwIBAQNiAAShziPCcOjKP6lZL1eCZwpuZRnJpqg7HYoMmu97k1wTQDgsg6gT6juf\n" +
                "cqCpWFHWc7Nn40+ewBIeg2VS5ErkOa/3krrUQEOt8mlcsdrm/HoYBNvNWVxacckf\n" +
                "bjPxMvziNDajggFXMIIBUzArBgNVHREEJDAigQ5wa2lAbW9mYS5nby5qcKQQMA4x\n" +
                "DDAKBgNVBAcMA0pQTjArBgNVHRIEJDAigQ5wa2lAbW9mYS5nby5qcKQQMA4xDDAK\n" +
                "BgNVBAcMA0pQTjAdBgNVHQ4EFgQUarTykoK9lkf2/yoC95RNdJ6XhGMwDgYDVR0P\n" +
                "AQH/BAQDAgEGMCsGA1UdEAQkMCKADzIwMTkwNjE5MDUxNTE5WoEPMjAzNTA2MTkw\n" +
                "NTE1MTlaMBgGA1UdIAQRMA8wDQYLKoMIho9+BgUBAQEwEgYDVR0TAQH/BAgwBgEB\n" +
                "/wIBADBtBgNVHR8EZjBkMDCgLqAshipodHRwczovL3BrZGRvd25sb2FkMS5pY2Fv\n" +
                "LmludC9DUkxzL0pQTi5jcmwwMKAuoCyGKmh0dHBzOi8vcGtkZG93bmxvYWQyLmlj\n" +
                "YW8uaW50L0NSTHMvSlBOLmNybDAMBggqhkjOPQQDAwUAA2kAMGYCMQCqewlcwiws\n" +
                "GJ/lCsLSSwn6JfDlgWBFF9T6/GASt/wa944f32wULLri+PXbq0PmMdcCMQDr7Xk7\n" +
                "9rnMW1p+e0/lApBGlpcRZhwgY5FwKSQWeWufbQoBzV89A8eHNsoNJ7H4vLw=\n", 0)

        const val JAPAN_HASH = "493260e5d98322b0cf61eabcaf5fe34e182a9b82f862f8555e431a8268f984f1"

        const val JAPAN_CRL_URL = "https://pkddownload1.icao.int/CRLs/JPN.crl"

        //prototype
        val protoCsca: ByteArray = Base64.decode( "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", 0)
        val protoCrl: ByteArray = Base64.decode("MIIC3zCByAIBATANBgkqhkiG9w0BAQsFADBlMQswCQYDVQQGEwJBVTEMMAoGA1UECgwDR09WMQ0wCwYDVQQLDARERkFUMQwwCgYDVQQLDANBUE8xKzApBgNVBAMMIlBhc3Nwb3J0IENvdW50cnkgU2lnbmluZyBBdXRoIFRFU1QXDTIxMDgxMTA0NTUyMloXDTIxMTEwOTA1MDAyMlqgLzAtMAoGA1UdFAQDAgEBMB8GA1UdIwQYMBaAFA4yoFknxNTLWHykRlXS61mkjsnZMA0GCSqGSIb3DQEBCwUAA4ICAQCktovZzLoMGX+dcGtIR0gFiOhNTIj4rgrL/RkAowvWCt4uj28DeumdGAO86owDbqJr7LZJ4lGVNE1wnLSmgqH4d1Zyv8kGZznTer5flXbl5+SKVnrVDB7QQyyu7cylIxOXc9iP3IwatmtkoT343yjOkIFEITdG3Fvk1EOKUS1J0MrwrhCRSCBT4bsweGL0jLDk2OccB2gGIR9Xqh4scZGWt2kwtGzWAREZruQ24XvKLCwchaVMV1SoRFIc5b6sNGE2UtOExgx+PjebVP9Qsz1a7ktfzJhtUSS+ssHW5Tb5H+T5pj+yjWqyK/q2pkXeHdVeczyY3wYbucqobpWsmXYIw1Oe/p7Od1tgXoTMvXBdU1WwVOi34sIApCVD8sMOMm06rd1f86KOWfkXNxjMi6zhs6zv3FWIhMSuvNJDOzvENw0L2gBfO/C4+yN1AshoG6C4PwV4tv4+WyT0nsOsQUUf7hVN1I7R0IeKwKXSeMyHy56c9ZxNJj4YdIXpgM9b5npPn02fJAHn60ue/8OI3VjqSlbj/TFNQGtmbSndggCw1npVOxsIwrpvR3n1wpfhRNmOWin8IlU31epDYrI82eCDnZgcsNvwo4qR4jb9RJDukKigjvI4fhoGm/eXMv+f1efPdFhKiOtaGvT6a7QDxaR9HK0CocCIOdjMMdR7MUlfYQ==", 0)
        val protoHash: String = "f6cb6f322e51423734351b4d398ba22abee768642e7caac5bc6366a486b0c2ea"

        // SHA256 hash of our CSCA certificate data
        const val cscaCertSHA256Hash = "35623a89f6b40cb4888e05147f7d16d27f7bf754aad563c37fef6e1a0e7ff5cf"

        // CSCA certificate
        val cscaCertData: ByteArray = Base64.decode("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", 0)

        // CRL
        val crlData: ByteArray = Base64.decode("MIIC3zCByAIBATANBgkqhkiG9w0BAQsFADBlMQswCQYDVQQGEwJBVTEMMAoGA1UECgwDR09WMQ0wCwYDVQQLDARERkFUMQwwCgYDVQQLDANBUE8xKzApBgNVBAMMIlBhc3Nwb3J0IENvdW50cnkgU2lnbmluZyBBdXRob3JpdHkXDTIxMDgxOTAzNTY1NVoXDTIxMTExNzAzNTY1NVqgLzAtMAoGA1UdFAQDAgFAMB8GA1UdIwQYMBaAFDYXwef1Z5VxLjd1cI5VgzGG6TgOMA0GCSqGSIb3DQEBCwUAA4ICAQDFCv8xG3oM0enVAP/7W4IDrbbyq8yZLPX4kbWqfAnaveLgMjHu+eo9aLDELjDHK859W4cguBEDmTZ9ewJ1eiJouvveLETrUnIn4dHEo+AKVuClvkYFPI8iexZl+kX1GzQpLBQLTTUA4PAskLF+YsDBMZ4eE66A2U5q17yFVsHFgwXKj0rKXbLsD+IvJkZKEDea5wJMj0wgaeF+y32t3rQy5Bi/0hMe95YXonlW33mxLOMQ6fbzEJeibwJ393QTvw4Y9Ohy6kMjaEVlxVuyVmtJaquW4ITI3idQTsQlZBI75BBH/nVAru38hgFEb6gmnMz4d4tUG/9PSvVhyHEHDd8g/OJGEkDnFDsNPh0e62Jkj9b0cE5bcWHDXmWtj33BOXA2jxuVE5aOzQRtkroiiarnwXrndZgh0y5WndpGNzsxkne+hXC09tvY0pnJ/DixP8A2vv33vzWd0JjEVTG1VuJJUIDvmXepMuKMsEwCfMrDgVd89uepLke61w/atA++MhB30/sU/8T+vrqAy/GUQGHRpdy8oQ1LS4yFbeWBxzlEpodq55SJisxPsup97+8QHjjJx+F3ELk6GkCv/M6ppplbmvkfW2rG2ll4O0VAtLlvykHhNEyzzwBjZA/IWmd/xwaBaHboq72LKGc1Hhr7VYRBfBodiPf1S6UAX4gbuDcmmg==", 0)

        // Valid VDS JSON
        const val validVDSJson = """
{
	"data": {
		"hdr": {
			"is": "AUS",
			"t": "icao.vacc",
			"v": 1
		},
		"msg": {
			"pid": {
				"dob": "1977-05-16",
				"i": "PF0911009",
				"n": "CITIZEN  JANE CATHERINE",
				"sex": "F"
			},
			"uvci": "VB0009990038",
			"ve": [{
				"des": "XM68M6",
				"dis": "RA01.0",
				"nam": "Pfizer Comirnaty",
				"vd": [{
					"adm": "Community Health",
					"ctr": "AUS",
					"dvc": "2021-06-13",
					"lot": "EP2163",
					"seq": 1
				}, {
					"adm": "General Practitioner",
					"ctr": "AUS",
					"dvc": "2021-07-01",
					"lot": "ER7449",
					"seq": 2
				}]
			}]
		}
	},
	"sig": {
		"alg": "ES256",
		"cer": "MIIDhDCCAWygAwIBAgICGK0wDQYJKoZIhvcNAQELBQAwZTELMAkGA1UEBhMCQVUxDDAKBgNVBAoMA0dPVjENMAsGA1UECwwEREZBVDEMMAoGA1UECwwDQVBPMSswKQYDVQQDDCJQYXNzcG9ydCBDb3VudHJ5IFNpZ25pbmcgQXV0aG9yaXR5MB4XDTIxMDgzMTE0MDAwMFoXDTMxMDkzMDEzNTk1OVowHDELMAkGA1UEBhMCQVUxDTALBgNVBAMTBERGQVQwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARSVpOyHuLjm01TB1iLBr3SrUp2GkQlM-mPqubbW3mjs0DTeRKrfVTSkkZNgOGj_DB_fo3p8qGy8UVgT4DQRVhIo1IwUDAWBgdngQgBAQYCBAswCQIBADEEEwJOVjAVBgNVHSUBAf8ECzAJBgdngQgBAQ4CMB8GA1UdIwQYMBaAFDYXwef1Z5VxLjd1cI5VgzGG6TgOMA0GCSqGSIb3DQEBCwUAA4ICAQCh_Qc5i6-vewGqinR9EdUpsl0P4jqg0pdx7hyOtPgYOwbTOegJyZOjyWZyuLlxGYuvCHqbrnATMedoIoUJzt8GxHA-4v5TUN2yEbRFXev8ur_0Y3uF4WXFr93Zl0LV78PBNZwXKfZEC6oTN_eVgtR37GdnYsWno0SuhR4fJo8JC_blivas8BJt78Hg8VhvWSK3uT0T58eYQjQhbsXV-BxJ2kSspdvkUF6-arLHh6DVS3ATPAGIm6fEvF4AxnLq5OSHOC3zZR0SR9XntYxEwjo_bW8O0Se8qa5mIBpXmvlwh0Ij6sqVwEskvkM30GmQGfZh5VjFujN2AZnwpjOjK0R-JvR3u6jsBJqVMgm75HgezOzayNiaqzhitrgg5KpO3gK_j3C-Doj5iPAm7I_63GyjUi8ZnqVUZ37UxM19uX2SvhTTQ70nL-zHNfHOyBXJgzMi4Zkor2uagHPz-W1XvNVwGEfFAu-nEyIOKBndHwnvSomL54yBv83X2yAQsoYggU18LNXMHUonTJ_ug7FU0LEX3qA1TeARJ4WBFNjysrBXQepVLowcbtvrhLFjocHjmCp3z17xUoKGI6daajCbvedXgeeSWSD5CuMAXpdN3Yml7VdW7PCK4DD0E_raw6d_wKNGSYAh0TBpNLxnunquai-gFIjgf4iRoys5F35KwmvpZw==",
		"sigvl": "FM5nk4TntHUSrVewwK3mqhmyXxAlJgKbu2qt9ZZI6U56PKVkzfDmIQKhURAlvnCKv9SfzmgtLCUxQFpbf2EuXQ=="
	}
}
"""

        // Invalid VDS JSON - identical to the valid JSON, except the name was modified (JANE to JANEX) to make the signature invalid
        const val invalidVDSJson = """
{
	"data": {
		"hdr": {
			"is": "AUS",
			"t": "icao.vacc",
			"v": 1
		},
		"msg": {
			"pid": {
				"dob": "1977-05-16",
				"i": "PF0911009",
				"n": "CITIZEN  JANEX CATHERINE",
				"sex": "F"
			},
			"uvci": "VB0009990038",
			"ve": [{
				"des": "XM68M6",
				"dis": "RA01.0",
				"nam": "Pfizer Comirnaty",
				"vd": [{
					"adm": "Community Health",
					"ctr": "AUS",
					"dvc": "2021-06-13",
					"lot": "EP2163",
					"seq": 1
				}, {
					"adm": "General Practitioner",
					"ctr": "AUS",
					"dvc": "2021-07-01",
					"lot": "ER7449",
					"seq": 2
				}]
			}]
		}
	},
	"sig": {
		"alg": "ES256",
		"cer": "MIIDhDCCAWygAwIBAgICGK0wDQYJKoZIhvcNAQELBQAwZTELMAkGA1UEBhMCQVUxDDAKBgNVBAoMA0dPVjENMAsGA1UECwwEREZBVDEMMAoGA1UECwwDQVBPMSswKQYDVQQDDCJQYXNzcG9ydCBDb3VudHJ5IFNpZ25pbmcgQXV0aG9yaXR5MB4XDTIxMDgzMTE0MDAwMFoXDTMxMDkzMDEzNTk1OVowHDELMAkGA1UEBhMCQVUxDTALBgNVBAMTBERGQVQwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARSVpOyHuLjm01TB1iLBr3SrUp2GkQlM-mPqubbW3mjs0DTeRKrfVTSkkZNgOGj_DB_fo3p8qGy8UVgT4DQRVhIo1IwUDAWBgdngQgBAQYCBAswCQIBADEEEwJOVjAVBgNVHSUBAf8ECzAJBgdngQgBAQ4CMB8GA1UdIwQYMBaAFDYXwef1Z5VxLjd1cI5VgzGG6TgOMA0GCSqGSIb3DQEBCwUAA4ICAQCh_Qc5i6-vewGqinR9EdUpsl0P4jqg0pdx7hyOtPgYOwbTOegJyZOjyWZyuLlxGYuvCHqbrnATMedoIoUJzt8GxHA-4v5TUN2yEbRFXev8ur_0Y3uF4WXFr93Zl0LV78PBNZwXKfZEC6oTN_eVgtR37GdnYsWno0SuhR4fJo8JC_blivas8BJt78Hg8VhvWSK3uT0T58eYQjQhbsXV-BxJ2kSspdvkUF6-arLHh6DVS3ATPAGIm6fEvF4AxnLq5OSHOC3zZR0SR9XntYxEwjo_bW8O0Se8qa5mIBpXmvlwh0Ij6sqVwEskvkM30GmQGfZh5VjFujN2AZnwpjOjK0R-JvR3u6jsBJqVMgm75HgezOzayNiaqzhitrgg5KpO3gK_j3C-Doj5iPAm7I_63GyjUi8ZnqVUZ37UxM19uX2SvhTTQ70nL-zHNfHOyBXJgzMi4Zkor2uagHPz-W1XvNVwGEfFAu-nEyIOKBndHwnvSomL54yBv83X2yAQsoYggU18LNXMHUonTJ_ug7FU0LEX3qA1TeARJ4WBFNjysrBXQepVLowcbtvrhLFjocHjmCp3z17xUoKGI6daajCbvedXgeeSWSD5CuMAXpdN3Yml7VdW7PCK4DD0E_raw6d_wKNGSYAh0TBpNLxnunquai-gFIjgf4iRoys5F35KwmvpZw==",
		"sigvl": "FM5nk4TntHUSrVewwK3mqhmyXxAlJgKbu2qt9ZZI6U56PKVkzfDmIQKhURAlvnCKv9SfzmgtLCUxQFpbf2EuXQ=="
	}
}
"""

        // Corrupt/non-VDS JSON
        const val nonVDSJson = """
{
    "isThisAVDS": false
}
"""
    }
}