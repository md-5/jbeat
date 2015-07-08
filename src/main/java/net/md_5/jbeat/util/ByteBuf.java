/**
 * The MIT License
 * Copyright (c) 2015 Techcable
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.md_5.jbeat.util;

import java.nio.ByteOrder;

public interface ByteBuf {

    /**
     * Get the array that currently backs this byte buffer
     * <p>
     * Modifications to the backing array may or may not be reflected in the byte buf
     * Modifications to the byte buffer may or may not be reflected in the array
     * </p>
     *
     * @return the array that backs this byte buffer
     *
     * @throws java.lang.UnsupportedOperationException if an array doesn't back this byte buffer
     */
    public byte[] array();

    /**
     * Reads the byte at the buffers current position, and then increments the position
     *
     * @return the byte at the buffer's current position
     *
     * @throws java.lang.IndexOutOfBoundsException if there are not enough bytes in the buffer
     */
    public byte read();

    /**
     * Reads the next four bytes in the buffer and turns them into an int, and then increments the position by 4
     *
     * @return the integer at the buffer's current position
     *
     * @throws java.lang.IndexOutOfBoundsException if there are not enough bytes in the buffer
     */
    public int readInt();

    /**
     * Reads the byte at the specified position
     *
     * @param index where to read from
     *
     * @return the byte at the buffer's current position
     *
     * @throws java.lang.IndexOutOfBoundsException if the index is past the readable bytes
     */
    public byte read(int index);

    /**
     * Read the contents of the buffer into the specified array
     *
     * @param bytes the array to fill
     *
     * @throws java.lang.IndexOutOfBoundsException if the buffer runs out of bytes mid-copy
     */
    public void read(byte[] bytes);

    /**
     * Write the contents of the array into the buffer, starting at the specified offset in the source array, and reading "length" bytes
     *
     * @param bytes the array to read from
     * @param offset where to start copying
     * @param length how much to read from the array
     *
     * @throws java.lang.IndexOutOfBoundsException if the buffer is bounded, and the next byte is past capacity
     * @throws java.lang.IllegalArgumentException if offset is larger than array.length or negative, or length is negative or larger than array.length - offset
     */
    public void write(byte[] bytes, int offset, int length);

    /**
     * Add the byte to the end of the buffer
     *
     * @param b the byte to write
     *
     * @throws java.lang.IndexOutOfBoundsException if the buffer is bounded, and the next byte is past capacity
     */
    public void write(byte b);

    /**
     * Write the four bytes in the specified int to the end of the buffer
     *
     * @param i the integer to write
     *
     * @throws java.lang.IndexOutOfBoundsException if the buffer is bounded, and the next byte is past capacity
     */
    public void writeInt(int i);

    /**
     * Gets the current position of the ByteBuf
     *
     * @return the current position of the byte buf
     *
     * @throws java.lang.IndexOutOfBoundsException if the index is past the readable bytes of the buffer
     */
    public long getPosition();

    /**
     * Reset the ByteBuf's position to zero
     */
    public void reset();

    /**
     * Read a variable length long from the stream, incrementing position the number of bytes specified by the integer
     *
     * @return a variable length long
     *
     * @throws java.lang.IndexOutOfBoundsException if the buffer runs out of bytes
     */
    public long readVarLong();

    /**
     * Read a UTF-8 string from the byte buf, starting with the length of the string, with the data following comprising the data of the string
     * <p>
     * Increments position by the number of bytes read
     * </p>
     *
     * @return a UTF-8 string
     */
    public String readString();

    /**
     * Set the order of all the bytes in the buffer
     *
     * @param order the new order of all the bytes in the buffer
     *
     * @throws java.lang.UnsupportedOperationException if not supported
     */
    public void setOrder(ByteOrder order);

    /**
     * Create a new byte buffer with the same backing bytes, but independent position
     *
     * @return a new byte buffer with the same backing bytes, but independent position
     */
    public ByteBuf duplicate();

    /**
     * Return the number of bytes currently in the buffer
     *
     * @return the number of bytes currently in the buffer
     */
    public long readableBytes();

}
