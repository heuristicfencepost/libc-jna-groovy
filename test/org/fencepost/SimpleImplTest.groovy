package org.fencepost

import groovy.util.Proxy

import com.sun.jna.Library
import com.sun.jna.NativeLibrary

import org.fencepost.struct.linux.Rlimit

/**
 * Test of a simple implementation of adding support for arbitrary calls to libc
 * when using JNA.
 *
 * @author mersault
 */
class SimpleImplTest extends GroovyTestCase {

    void testExample1() {

        /* Create the lib first so that it's in scope when the closure is declared */
        def libc = NativeLibrary.getInstance("c")

        /* We do not wish to register this meta-class; other instances of NativeLibrary
        may wish to do things differently. */
        ExpandoMetaClass emc = new ExpandoMetaClass(NativeLibrary,false)
        emc.methodMissing = {

            String fname, fargs ->
                println "Invoking method name ${fname}, args: ${fargs}"
                def f = libc.getFunction(fname)

                /* These functions do not follow the standard calling convention; they are
                 * always successful. */
                if (["getuid","geteuid","getgid","getegid","getpid","getppid"].contains(fname)) {

                    synchronized (libc) {

                        return f.invokeInt(fargs)
                    }
                }
                else {

                    /* Standard libc convention is to return an integer with a value of
                    zero if the call was successful and some non-zero value otherwise.  If
                    a non-zero value is returned errno is set appropriately. */
                    synchronized (libc) {

                        def rv = f.invokeInt(fargs)
                        if (rv == 0) { return rv }
                        else {

                            def errnoptr = libc.getGlobalVariableAddress("errno")
                            def errno = errnoptr.getInt(0)
                            def errstr = libc.getFunction("strerror").invokeString([errno] as Object[],false)
                            throw new LibcException(errno,errstr)
                        }
                    }

                }
        }
        emc.initialize()

        def libcproxy = new Proxy().wrap(libc)
        libcproxy.setMetaClass(emc)

        /* Test a few functions which don't follow the normal libc calling convention */
        def pid = libcproxy.getpid()
        println "PID: ${pid}}"
        assert pid > 0

        def uid = libcproxy.getuid()
        println "UID: ${uid}"
        assert uid > 0

        def gid = libcproxy.getgid()
        println "GID: ${gid}"
        assert gid > 0

        /* Check a function which does follow the calling convention */
        def rlimit = new Rlimit()
        /* RLIMIT_NOFILE = 7 on Linux */
        def rv = libcproxy.getrlimit(7,rlimit)
        println "rlimit: ${rlimit}"
        assert rlimit.curr > 0
        assert rlimit.max > 0

        /* Check a function which follows the convention while inducing an error */
        try {

            rlimit = new Rlimit()
            /* 700 is well outside the list of acceptable resources for getrlimit */
            rv = libcproxy.getrlimit(700,rlimit)
            assert false
        }
        catch (LibcException le) {

            println "Hit exception (expected): ${le}"
            assert true
        }
    }
}

