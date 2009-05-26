package org.fencepost

import com.sun.jna.NativeLibrary

/**
 * Wrapper around a NativeLibrary representing libc which makes the lib function
 * as a Groovy object.
 *
 * This still isn't a flawless approach:
 *
 * 1. We have to implement a Groovy class for a specific native lib (libc) rather than
 * a general Groovy wrapper around JNA's NativeLibrary.  This is a bit misleading;
 * a better way to express this constraint might be that we're forced to implement
 * a Groovy class corresponding to this calling convention, a convention that happens
 * to be used by libc.  In theory we could re-use this class for any native lib
 * which followed the same convention.
 *
 * 2. There's no obvious way to verify that the NativeLibrary instance passed to our
 * constructor is actually a front-end for libc.
 *
 * @author h.fencepost
 */
class GroovyLibc extends GroovyObjectSupport {

    /* Use the access control modifiers here even though they're ignored; see below */
    private libc
    private final alwaysSuccessfulFuncs = ["getuid","geteuid","getgid","getegid","getpid","getppid"]

    public GroovyLibc(NativeLibrary arg) {

        libc = arg
    }

    /* Complete hack to cover the fact that the private access control modifier for properties is
    apparently completely ignored now.  Details can be found at http://jira.codehaus.org/browse/GROOVY-1875 */
    public Object getProperty(String name) {

        switch (name) {
            case "libc": throw new MissingPropertyException("Property ${name} unknown")
            case "alwaysSuccessfulFuncs": throw new MissingPropertyException("Property ${name} unknown")
            default: return super.getProperty(name)
        }
    }

    public Object invokeMethod(String name, Object args) {

        println "Invoking method name ${name}, args: ${args}"
        def f = libc.getFunction(name)
        if (f == null) {

            throw new MissingMethodException("Could not find function ${name}")
        }

        if (alwaysSuccessfulFuncs.contains(name)) {

            synchronized (libc) {

                return f.invokeInt(args)
            }
        }
        else {

            synchronized (libc) {

                def rv = f.invokeInt(args)
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
}