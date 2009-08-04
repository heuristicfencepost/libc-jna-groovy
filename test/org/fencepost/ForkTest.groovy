package org.fencepost

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.ptr.IntByReference

/**
 * Proof-of-concept to illustrate how much fun forking can be with just
 * a bit of Groovy.
 *
 * @author h.fencepost
 */
class ForkTest extends GroovyTestCase {

    void testFork() {
        
        def libc = NativeLibrary.getInstance("c")
        def groovylibc = new GroovyLibc(libc)

        /* Parent gets the PID, child gets zero */
        /* Note that vfork() does not work here (at least for Linux), presumably
           because of the sharing of resources. */
        def forkval = groovylibc.fork()
        if (forkval == 0) {

            /* Would you like to use PATH?  Both calls below should work (assuming
               an install of vim at /usr/bin) */
            //groovylibc.execl("/usr/bin/vim","/usr/bin/vim",null)
            groovylibc.execlp("vim","vim",null)
        }
        else {

            def iref = new IntByReference()
            def pid = groovylibc.wait(iref)
            println "Child ${forkval} exited with status ${iref.value}"
        }
    }
}

