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

import lombok.*;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
class NIOByteBuf implements ByteBuf {

    private final ByteBuffer backing;

    public byte[] array() {
        return backing.array();
    }

    public byte read() {
        try {
            return backing.get();
        } catch (BufferUnderflowException ex) {
            throw new IndexOutOfBoundsException("Current position past buffer limit");
        }
    }

    public int readInt() {
        try {
            return backing.getInt();
        } catch (BufferUnderflowException ex) {
            throw new IndexOutOfBoundsException("Current position past buffer limit");
        }
    }

    public byte read(int index) {
        return backing.get(index); // Throws an IndexOutOfBoundsException on index out of bounds
    }

    public void read(byte[] bytes) {
        try {
            backing.get(bytes);
        } catch (BufferUnderflowException ex) {
            throw new IndexOutOfBoundsException("Not enough bytes left to fill byte array");
        }
    }

    public void write(byte[] bytes, int offset, int length) {
        try {
            backing.put(bytes, offset, length);
        } catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException(ex);
        } catch (BufferOverflowException e) {
            throw new IndexOutOfBoundsException("Not enough bytes in the buffer");
        }
    }

    public void write(byte b) {
        try {
            backing.put(b);
        } catch (BufferOverflowException e) {
            throw new IndexOutOfBoundsException("Current position past buffer limit");
        }
    }

    public void writeInt(int i) {
        try {
            backing.putInt(i);
        } catch (BufferOverflowException e) {
            throw new IndexOutOfBoundsException("Current position past buffer limit");
        }
    }

    public long getPosition() {
        return backing.position();
    }

    public void reset() {
        backing.rewind();
    }

    public long readVarLong() {
        return ByteBufs.readVarLong(this);
    }

    public String readString() {
        int length = (int) readVarLong();
        String ret = null;
        if (length != 0) {
            int limit = backing.limit();
            backing.limit(backing.position() + length);
            ret = StandardCharsets.UTF_8.decode(backing).toString();
            backing.limit(limit);
        }
        return ret;
    }

    public void setOrder(ByteOrder order) {
        backing.order(order);
    }

    public ByteBuf duplicate() {
        ByteBuffer duplicated = backing.duplicate();
        return ByteBufs.wrap(duplicated);
    }

    public long readableBytes() {
        return backing.capacity();
    }
}
