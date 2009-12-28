package org.fencepost

import com.sun.jna.NativeLibrary

/**
 * Wrapper around a NativeLibrary representing glibc which makes the lib function
 * as a Groovy object.
 *
 * We use the access control modifiers for class properties even though they're apparently
 * ignored; see getProperty() for more detail.
 *
 * @author h.fencepost
 */
class GroovyLibc extends GroovyObjectSupport {

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
        def f = libc.getFunction(name)
        if (f == null) {

            throw new MissingMethodException("Could not find function ${name}")
        }

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