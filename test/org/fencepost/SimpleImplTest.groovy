package org.fencepost

import groovy.util.Proxy

import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.NativeLibrary

import org.fencepost.struct.linux.Rlimit
import org.fencepost.struct.linux.Utsname

/**
 * Test of a simple implementation of adding support for arbitrary calls to libc
 * when using JNA.
 *
 * @author h.fencepost
 */
class SimpleImplTest extends GroovyTestCase {

    void testSimple() {

        /* Create the lib first so that it's in scope when the closure is declared */
        def libc = NativeLibrary.getInstance("c")

        /* We do not wish to register this meta-class; other instances of NativeLibrary
        may wish to do things differently. */
        ExpandoMetaClass emc = new ExpandoMetaClass(Proxy,false)
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

                    /* Standard libc calling convention depends on the syscall involved.
                     * Some syscalls are useful only for their side effects (i.e. setting
                     * a value or set of values in some input struct); these return 0 on
                     * success and -1 (with errno set appropriately) on failure.  Other
                     * syscalls return a value that has some meaning; open() returns
                     * the new file descriptor, fork() returns the child PID in the
                     * parent etc.  Errors in these calls are also marked by a return
                     * value of -1 and a correct value for errno.
                     *
                     * We can cover both cases by checking for -1 in the return value
                     * and returning that value if it's anything else.  If the call is
                     * useful only for it's side effects this return value can be safely
                     * ignored. */
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

        /* Verify that NativeLibrary methods are still accessible */
        def func = libcproxy.adaptee.getFunction("fork")
        assert func != null
        assert func instanceof Function

        /* One more function call, just for fun */
        def utsname = new Utsname()
        rv = libcproxy.uname(utsname)
        println "utsname sysname: ${new String(utsname.sysname)}"
        println "utsname release: ${new String(utsname.release)}"
        println "utsname version: ${new String(utsname.version)}"
    }
}

