package org.fencepost

import groovy.util.Proxy

import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.NativeLibrary

import org.fencepost.struct.linux.Rlimit
import org.fencepost.struct.linux.Utsname

/**
 * Test of a better implementation of calling glibc using JNA.
 *
 * Verified with:
 * Groovy 1.7.0 (Java 1.6.0_16)
 * JNA 3.2.4
 * Ubuntu 9.10
 * 
 * @author h.fencepost
 */
class EvenBetterImplTest extends GroovyTestCase {

    void testEvenBetter() {

        def groovylibc = new BetterGroovyLibc()

        /* Test a few functions which do not have side effects */
        def pid = groovylibc.getpid()
        println "PID: ${pid}}"
        assert pid > 0

        def uid = groovylibc.getuid()
        println "UID: ${uid}"
        assert uid > 0

        def gid = groovylibc.getgid()
        println "GID: ${gid}"
        assert gid > 0

        /* Test a few functions that have side effects.  7 is the integer value
         * for RLIMIT_NOFILE on Linux */
        def rlimit = new Rlimit()
        def rv = groovylibc.getrlimit(7,rlimit)
        println "rlimit: ${rlimit}"
        assert rlimit.curr > 0
        assert rlimit.max > 0

        def utsname = new Utsname()
        rv = groovylibc.uname(utsname)
        println "utsname sysname: ${new String(utsname.sysname)}"
        println "utsname release: ${new String(utsname.release)}"
        println "utsname version: ${new String(utsname.version)}"

        /* Another check for a function with side effects, this time with parameters
         * which should induce an error (700 is well outside the list of acceptable
         * resources for getrlimit() */
        try {

            rlimit = new Rlimit()
            rv = groovylibc.getrlimit(700,rlimit)
            assert false
        }
        catch (LibcException le) {

            println "Hit exception (expected): ${le}"
            assert true
        }

        /* Verify that NativeLibrary is now hidden  */
        try {

            def func = groovylibc?.libc?.getFunction("fork")
            assert false
        }
        catch (MissingPropertyException mpe) {

            println "Hit exception (expected): ${mpe}"
            assert true
        }
    }
}
