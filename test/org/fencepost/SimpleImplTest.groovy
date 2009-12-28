package org.fencepost

import groovy.util.Proxy

import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.NativeLibrary

import org.fencepost.struct.linux.Rlimit
import org.fencepost.struct.linux.Utsname

/**
 * Test of a simple implementation of calling glibc using JNA.
 *
 * Verified with:
 * Groovy 1.7.0 (Java 1.6.0_16)
 * JNA 3.2.4
 * Ubuntu 9.10
 *
 * @author h.fencepost
 */
class SimpleImplTest extends GroovyTestCase {

    void testSimple() {

        /* Create the lib first so that it's in scope when the closure is declared */
        def libc = NativeLibrary.getInstance("c")

        /* No need to register this meta-class; other instances of NativeLibrary may wish
         * to do things differently. */
        ExpandoMetaClass emc = new ExpandoMetaClass(Proxy,false)
        emc.methodMissing = {

            String fname, fargs ->
                println "Invoking method name ${fname}, args: ${fargs}"
                def f = libc.getFunction(fname)

                /* Within glibc some functions return an interesting value while some are called only
                 * for side effects.  In both cases most (all?) functions return -1 to indicate an
                 * error and set the global integer errno in order to indicate the underlying
                 * cause of the error.
                 *
                 * We can cover both cases by checking for -1 in the return value and returning that
                 * value if it's anything else.  If the call is useful only for it's side effects
                 * this return value can be safely ignored. */
                synchronized (libc) {

                    def rv = f.invokeInt(fargs)
                    if (rv == -1) {

                        def errnoptr = libc.getGlobalVariableAddress("errno")
                        def errno = errnoptr.getInt(0)
                        def errstr = libc.getFunction("strerror").invokeString([errno] as Object[],false)
                        throw new LibcException(errno,errstr)
                    }
                    else { return rv }
                }
        }
        emc.initialize()

        def libcproxy = new Proxy().wrap(libc)
        libcproxy.setMetaClass(emc)

        /* Test a few functions which do not have side effects */
        def pid = libcproxy.getpid()
        println "PID: ${pid}}"
        assert pid > 0

        def uid = libcproxy.getuid()
        println "UID: ${uid}"
        assert uid > 0

        def gid = libcproxy.getgid()
        println "GID: ${gid}"
        assert gid > 0

        /* Test a few functions that have side effects.  7 is the integer value
         * for RLIMIT_NOFILE on Linux */
        def rlimit = new Rlimit()
        def rv = libcproxy.getrlimit(7,rlimit)
        println "rlimit: ${rlimit}"
        assert rlimit.curr > 0
        assert rlimit.max > 0

        def utsname = new Utsname()
        rv = libcproxy.uname(utsname)
        println "utsname sysname: ${new String(utsname.sysname)}"
        println "utsname release: ${new String(utsname.release)}"
        println "utsname version: ${new String(utsname.version)}"

        /* Another check for a function with side effects, this time with parameters
         * which should induce an error (700 is well outside the list of acceptable
         * resources for getrlimit() */
        try {

            rlimit = new Rlimit()
            rv = libcproxy.getrlimit(700,rlimit)
            assert false
        }
        catch (LibcException le) {

            println "Hit exception (expected): ${le}"
            assert true
        }

        /* Verify that NativeLibrary methods are still accessible */
        def func = libcproxy.adaptee.getFunction("fork")
        assert func != null
        assert func instanceof Function
    }
}
