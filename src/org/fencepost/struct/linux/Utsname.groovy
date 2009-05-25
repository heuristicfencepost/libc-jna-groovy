package org.fencepost.struct.linux

import com.sun.jna.Structure

/**
 * JNA-compatible implementation of the utsname struct defined in
 * /usr/include/sys/utsname.h.  Assuming the glibc extension of 
 * the domainname field.
 *
 * Field size fixed based on values in /usr/include/bits/utsname.h
 *
 * @author h.fencepost
 */
class Utsname extends Structure {
    public byte[] sysname = new byte[65]
    public byte[] nodename = new byte[65]
    public byte[] release = new byte[65]
    public byte[] version = new byte[65]
    public byte[] machine = new byte[65]
    public byte[] domainname = new byte[65]
}