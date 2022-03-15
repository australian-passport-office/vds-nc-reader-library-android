package au.gov.dfat.vdsncchecker.example

import au.gov.dfat.lib.vdsncchecker.VaultError

enum class BarcodeError {
    UVCI_PARSE_ERROR
}

class BarcodeException(val error: BarcodeError): Exception() {

}