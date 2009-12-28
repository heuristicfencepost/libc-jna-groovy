package org.fencepost

import com.sun.jna.Function
import com.sun.jna.LastErrorException
import com.sun.jna.NativeLibrary

/**
 * Wrapper around a NativeLibrary representing glibc which makes the lib function
 * as a Groovy object.  This is an extension of GroovyLibc which uses the
 * LastErrorException in the 3.2.x line of JNA.
 *
 * @author h.fencepost
 */
class BetterGroovyLibc extends GroovyObjectSupport {

    private libc = NativeLibrary.getInstance("c")

    /* Complete hack to cover the fact that the private access control modifier for properties is
    apparently completely ignored now.  Details can be found at http://jira.codehaus.org/browse/GROOVY-1875 */
    public Object getProperty(String name) {

        switch (name) {
            case "libc": throw new MissingPropertyException("Property ${name} unknown")
            default: return super.getProperty(name)
        }
    }

    public Object invokeMethod(String name, Object args) {

        println "Invoking method name ${name}, args: ${args}"
        try {

            def f = libc.getFunction(name,Function.THROW_LAST_ERROR)
            if (f == null) {

                throw new MissingMethodException("Could not find function ${name}")
            }

            return f.invokeInt(args)
        }
        catch (UnsatisfiedLinkError ule) {

            throw new MissingMethodException("Could not find function ${name}")
        }
        catch (LastErrorException lee) {

            def errno = lee.errorCode
            def errstr = libc.getFunction("strerror").invokeString([errno] as Object[],false)
            throw new LibcException(errno,errstr)
        }
    }
}