package au.gov.dfat.lib.vdsncchecker.utils

/**
 *
 * empty interface to allow any sort of data returned from Uvci processing
 *
 */
interface IUvciStatus{

}

/**
 *
 * interface for UVCI processor
 *
 * [isLengthInvalid] - checks if original value is of a suitable length to be processed
 *
 * [isCheckDigitValid] - checks the check digit
 *
 * [numberRangeMatchesExpected] - checks that the characters in the found value are as expected
 *
 * [processUvciResult] - processes result if value passed previous checks
 *
 * @property invalid - invalid status when error/invalid
 *
 */
interface IUvciProcessor<Status> where Status: IUvciStatus {
    fun processUvciResult(number: Int): Status
    fun isCheckDigitValid(value: String): Boolean
    fun isLengthInvalid(value:String): Boolean
    fun numberRangeMatchesExpected(value: String) : Boolean
    val invalid: Status
}

/**
 *
 * interface for easy extension where it is needed
 *
 */
interface IVdsMsg{
    val uvci: String
}

/**
 *
 * extension method for a VDS
 *
 * @receiver - VDS
 *
 * @param processor - UVCI processor specific to needs to app
 *
 */
fun <Status> IVdsMsg.checkUVCIRange(processor: IUvciProcessor<Status>) : Status where Status: IUvciStatus{

    val value = this.uvci

    if (processor.isLengthInvalid(value)) {
        return processor.invalid
    }

    // Get the value excluding the prefix and check digit
    val numberRange = value.substring(2, value.length - 1)

    if (!processor.isCheckDigitValid(value)) {
        return processor.invalid
    }

    if (!processor.numberRangeMatchesExpected(numberRange)) {
        return processor.invalid
    }

    val number = numberRange.toInt()

    return processor.processUvciResult(number)
}