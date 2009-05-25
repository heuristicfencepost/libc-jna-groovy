package org.fencepost

import groovy.util.Proxy

import com.sun.jna.Library
import com.sun.jna.NativeLibrary

import org.fencepost.struct.linux.Rlimit

/**
 *
 * @author mersault
 */
class Example1Test extends GroovyTestCase {

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

                /* Standard libc convention is to return an integer with a value of
                zero if the call was successful and some non-zero value otherwise.  If
                a non-zero value is returned errno is set appropriately.

                Assume this as a standard and only modify it for calls which are known
                to do something else. */
                return f.invokeInt(fargs)
        }
        emc.initialize()

        def libcproxy = new Proxy().wrap(libc)
        libcproxy.setMetaClass(emc)

        def pid = libcproxy.getpid()
        println "PID: ${pid}}"
        assert pid > 0

        def uid = libcproxy.getuid()
        println "UID: ${uid}"
        assert uid > 0

        def gid = libcproxy.getgid()
        println "GID: ${gid}"
        assert gid > 0

        def rlimit = new Rlimit()
        /* RLIMIT_NOFILE = 7 on Linux */
        def rv = libcproxy.getrlimit(7,rlimit)
        println "rlimit: ${rlimit}"
        assert rlimit.curr > 0
        assert rlimit.max > 0
    }
}

