package org.fencepost

import groovy.util.Proxy

import com.sun.jna.Library
import com.sun.jna.NativeLibrary

/**
 *
 * @author mersault
 */
class LibraryFactory {

	def getLibrary() {

        /* Create the lib first so that it's in scope when the closure is declared */
        def lib = NativeLibrary.getInstance("c")

        ExpandoMetaClass emc = new ExpandoMetaClass(NativeLibrary)
        emc.methodMissing = {

            String fname, fargs ->
                println "Invoking method name ${fname}, args: ${fargs}"
                def f = lib.getFunction(fname)
                fname == "getpid" ? f.invokeInt(fargs) : f.invoke(fargs)
        }
        emc.initialize()

        def libproxy = new Proxy().wrap(lib)
        libproxy.setMetaClass(emc)
        return libproxy
    }
}