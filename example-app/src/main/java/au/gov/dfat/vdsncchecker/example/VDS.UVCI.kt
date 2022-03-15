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

import au.gov.dfat.lib.vdsncchecker.VDS
import au.gov.dfat.lib.vdsncchecker.VDSVerifyError
import au.gov.dfat.lib.vdsncchecker.VDSVerifyException
import java.lang.Exception

/**
 *
 * for checking validity of UVCI
 *
 */
class UVCIChecker{

    companion object {
        /**
         *
         * check what range the UVCI falls under
         *
         * @param value - the UVCI to check
         *
         * @return UVCIRange enum representation
         *
         *
         */
        fun checkRange(value: String): UVCIRange{
            if(value.length < 4){
                return UVCIRange.Invalid
            }

            val testLimit = 998999
            val specimenLimit = 999999

            //get the number string, dont include the first two alpha chars, or the last check digit
            val numberStr =  value.substring(2,value.length - 1)

            //get the check digit, the last digit
            val checkDigit = value.last().toString()

            //validate the check digit
            if(!validateCheckDigit(value.substring(0, value.length - 1), checkDigit) ){
                return UVCIRange.Invalid
            }

            //check its numeric
            if (!isNumericAllowedCharacter(numberStr)) {
                return UVCIRange.Invalid
            }

            //convert to number
            //was guard in SWIFT
            val number = try{
                numberStr.toInt()

            }catch(exception: Exception){
                throw BarcodeException(BarcodeError.UVCI_PARSE_ERROR)
            }


            //now check the range
            if(number <= testLimit)
            {
                return UVCIRange.Test
            }

            if(number <= specimenLimit)
            {
                return UVCIRange.Specimen
            }

            return UVCIRange.Production
        }
    }

}

/**
 *
 * enum representations of UVCI ranges
 *
 */
enum class UVCIRange{
    Invalid,
    Test,
    Specimen,
    Production
}

/**
 *
 * extension function from a VDS object
 *
 */
fun VDS.UVCIRange(): UVCIRange{
    return UVCIChecker.checkRange(data.msg.uvci)
}

private const val set = "abcdefghijklmnopqrstuvwxyz"

/**
 *
 * check if numeric
 *
 * @param value - the string the check
 *
 * @return whether the character is an integer
 *
 */
private fun isNumericAllowedCharacter(value: String): Boolean {

    set.forEach { c ->
        if(value.indexOf(c, 0, true) > -1){
            return false
        }
    }
    return true
}

/**
 *
 * maps a character to an integer value
 *
 * @param character - the character to convert from
 *
 * @return the value the character converted to
 *
 */
private fun mapCharacter(character: String): UInt{
    val char = character.lowercase()
    if(set.indexOf(char) > -1){
        return (set.indexOf(char) + 10).toUInt()
    }
    val number = char.toIntOrNull()
    if(number != null){
        return number.toUInt()
    }
    return 0u
}

    /**
    * data validation function
    *
    * @param data The data that needs to be validated
    * @param check The checksum string for the validation
    *
    * @return Returns true if the data was valid
     * * */
private fun validateCheckDigit(data: String, check: String): Boolean {
    // The check digit calculation is as follows: each position is assigned a value; for the digits 0 to 9 this is
    // the value of the digits, for the letters A to Z this is 10 to 35, for the filler < this is 0. The value of
    // each position is then multiplied by its weight; the weight of the first position is 7, of the second it is 3,
    // and of the third it is 1, and after that the weights repeat 7, 3, 1, etcetera. All values are added together
    // and the remainder of the final value divided by 10 is the check digit.

    var i  = 1
    var dc = 0
    val w: Array<Int> = arrayOf(7, 3, 1)
    val b0: UInt = 0u
    val b9: UInt = 9u
    val bA: UInt = 10u
    val bZ: UInt = 35u
    val bK: UInt = 0u

    data.toCharArray().forEach { cha ->
        var d = 0
        val c: UInt = mapCharacter(cha.toString())


        when {
            c in b0..b9 -> {
                d = (c - b0).toInt()
            }
            c in bA..bZ -> {
                d = ((bA + c) - bA).toInt()
            }
            c != bK -> {
                return false
            }
        }
        dc += d * w[(i-1)%3]
        //increment
        i += 1
    }


    try {

        if( (dc%10) != check.toInt() ){
            return false
        }
    }catch(exception: Exception) {
        return false
    }

    return true
}
