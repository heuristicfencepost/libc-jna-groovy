package org.fencepost

/**
 *
 * @author mersault
 */
class LibraryFactoryTest extends GroovyTestCase {

    void testLibrary() {

        def factory = new LibraryFactory()
        def lib = factory.getLibrary()

        def pid = lib.getpid()
        println "PID: ${pid}}"
        assert pid > 0
    }
}

