package org.fencepost

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.ptr.IntByReference

/**
 * Proof-of-concept to illustrate how much fun forking can be with just
 * a bit of Groovy.
 *
 * Verified with:
 * Groovy 1.7.0 (Java 1.6.0_16)
 * JNA 3.2.4
 * Ubuntu 9.10
 * 
 * @author h.fencepost
 */
class ForkTest extends GroovyTestCase {

    void testFork() {
        
        def groovylibc = new BetterGroovyLibc()

        /* Parent gets the PID, child gets zero
         *
         * Note that vfork() does not work here (at least for Linux), presumably
         * because of the sharing of resources. */
        def forkval = groovylibc.fork()
        println "forkval: ${forkval}"
        if (forkval == 0) {

            println "execlp() in child, PID: ${forkval}"

            /* Would you like to use PATH?  Both calls below should work (assuming
               an install of vim at /usr/bin) */
            //groovylibc.execl("/usr/bin/vim","/usr/bin/vim",null)
            groovylibc.execlp("vim","vim",null)
        }
        else {

            /* On some platforms we have to make our threads sleep for a small amount of time
               before waiting on the child to complete.  It's not logically required for the
               code to work; perhaps a constraint of JNA performance? */
            sleep 200
            def iref = new IntByReference()
            def pid = groovylibc.wait(iref)
            println "Child ${forkval} exited with status ${iref.value}"
        }
    }
}

