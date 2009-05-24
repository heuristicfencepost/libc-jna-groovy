package fencepost.org

import groovy.util.Proxy

import com.sun.jna.NativeLibrary

/**
 *
 * @author mersault
 */
class LibraryFactory {

	def getLibrary() {

        def lib = NativeLibrary.getInstance("c")

        ExpandoMetaClass emc = new ExpandoMetaClass(NativeLibrary)
        emc.methodMissing = {

            String fname, fargs ->
                println "Invoking method name ${fname}, args: ${fargs}"
            def f = lib.getFunction(fname)
            fname == "getpid" ? f.invokeInt(fargs) : f.invoke(fargs)
            //lib.getFunction(fname).invoke(fargs)
        }
        emc.initialize()

        def libproxy = new Proxy().wrap(lib)
        libproxy.setMetaClass(emc)
        return libproxy
    }
}