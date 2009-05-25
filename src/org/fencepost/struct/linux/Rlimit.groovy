package org.fencepost.struct.linux

import com.sun.jna.Structure

/**
 * JNA-compatible implementation of the rlimit struct defined in
 * /usr/include/bits/resource.h
 *
 * @author h.fencepost
 */
class Rlimit extends Structure {
      public int curr
      public int max
}

