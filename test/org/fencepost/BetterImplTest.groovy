package org.fencepost

import groovy.util.Proxy

import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.NativeLibrary

import org.fencepost.struct.linux.Rlimit
import org.fencepost.struct.linux.Utsname

/**
 * Test of a somewhat better implementation of handling calls to libc functions
 * from within Groovy using JNA.
 *
 * @author h.fencepost
 */
class BetterImplTest extends GroovyTestCase {

    void testBetter() {

        /* Create the lib first so that it's in scope when the closure is declared */
        def libc = NativeLibrary.getInstance("c")
        def groovylibc = new GroovyLibc(libc)

        /* Test a few functions which don't follow the normal libc calling convention */
        def pid = groovylibc.getpid()
        println "PID: ${pid}}"
        assert pid > 0

        def uid = groovylibc.getuid()
        println "UID: ${uid}"
        assert uid > 0

        def gid = groovylibc.getgid()
        println "GID: ${gid}"
        assert gid > 0

        /* Check a function which does follow the calling convention */
        def rlimit = new Rlimit()
        /* RLIMIT_NOFILE = 7 on Linux */
        def rv = groovylibc.getrlimit(7,rlimit)
        println "rlimit: ${rlimit}"
        assert rlimit.curr > 0
        assert rlimit.max > 0

        /* Check a function which follows the convention while inducing an error */
        try {

            rlimit = new Rlimit()
            /* 700 is well outside the list of acceptable resources for getrlimit */
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

        /* One more function call, just for fun */
        def utsname = new Utsname()
        rv = groovylibc.uname(utsname)
        println "utsname sysname: ${new String(utsname.sysname)}"
        println "utsname release: ${new String(utsname.release)}"
        println "utsname version: ${new String(utsname.version)}"
    }
}

