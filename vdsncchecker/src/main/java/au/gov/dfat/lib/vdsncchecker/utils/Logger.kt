package au.gov.dfat.lib.vdsncchecker.utils

/**
 *
 * simple logger for debugging
 *
 */
open abstract class Logger {

    protected open val canLog: Boolean
    get() = false

    open fun printLine(message: String, prefix: String = "LOGGER:"){
        if(canLog){
            println("$prefix $message")
        }
    }
}