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

import java.nio.ByteBuffer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ByteBufs {

    /**
     * Create a new resizable byte buffer with the specified initial bytes
     *
     * @return a new resizable byte buffer with the specified inital bytes
     */
    public static ByteBuf create(byte[] bytes) {
        ByteBuf buf = create(bytes.length);
        buf.write(bytes, 0, bytes.length);
        return buf;
    }

    /**
     * Create a new resizable byte buffer
     *
     * @return a new resizable byte buffer
     */
    public static ByteBuf create() {
        return new ResizableByteBuf();
    }

    protected static long readVarLong(ByteBuf buf) {
        long data = 0, shift = 1;
        while (true) {
            byte x = buf.read();
            data += (x & 0x7F) * shift;
            if ((x & 0x80) != 0x00) {
                break;
            }
            shift <<= 7;
            data += shift;
        }
        return data;
    }

    /**
     * Create a new resizable byte buffer with the specified initial capacity
     *
     * @param initialCapacity the initial capacity of the new byte buf
     *
     * @return a new resizable byte buffer
     */
    public static ByteBuf create(int initialCapacity) {
        return new ResizableByteBuf(initialCapacity);
    }

    public static ByteBuf wrap(ByteBuffer backing) {
        return new NIOByteBuf(backing);
    }

}
