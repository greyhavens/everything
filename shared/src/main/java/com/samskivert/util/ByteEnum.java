//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.samskivert.util;

/**
 * An interface implemented by enums that can map themselves to a byte. See {@link ByteEnumUtil}
 * for mapping back from a byte to an enum.
 */
public interface ByteEnum
{
    /**
     * Returns the byte value to which to map this enum value.
     */
    public byte toByte ();
}
