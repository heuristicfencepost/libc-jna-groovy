package org.fencepost

/**
 * Representation of an "error" (usually a non-zero return value) to some
 * libc call.  Should contain a value for errno.
 *
 * @author mersault
 */
class LibcException extends Exception {

    private int errno
    private String errstr

    public LibcException(int arg0, String arg1) {

        super()
        errno = arg0
        errstr = arg1
    }

    def getErrno() { return errno }

    def getErrstr() { return errstr }

    String toString() { return "LibcException, errno: ${errno}, errstr: ${errstr}"}
}

