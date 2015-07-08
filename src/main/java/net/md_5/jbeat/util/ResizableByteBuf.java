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

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
class ResizableByteBuf implements ByteBuf {

    private final ByteList byteList;
    private int position;

    public ResizableByteBuf(int initialCapacity) {
        this(new ByteArrayList(initialCapacity));
    }

    public ResizableByteBuf() {
        this(new ByteArrayList());
    }

    public byte[] array() {
        return byteList.toArray();
    }

    public byte read() {
        byte read = byteList.get(position);
        position++;
        return read;
    }

    public int readInt() {
        byte b = read();
        byte b1 = read();
        byte b2 = read();
        byte b3 = read();
        return (((b) << 24) | ((b1 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b3 & 0xff)));
    }

    public byte read(int index) {
        return byteList.get(position);
    }

    public void read(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = read();
        }
    }

    public void write(byte[] bytes, int offset, int length) {
        if (offset < 0 || bytes.length > offset || length < 0 || bytes.length - offset > length) throw new IllegalArgumentException();
        for (int i = offset; i < offset + length; i++) {
            write(bytes[i]);
        }
    }

    public void write(byte b) {
        byteList.add(b);
    }

    public void writeInt(int i) {
        write((byte) (i >> 24));
        write((byte) (i >> 16));
        write((byte) (i >> 8));
        write((byte) i);
    }

    public long getPosition() {
        return position;
    }

    public void reset() {
        position = 0;
    }

    public long readVarLong() {
        return ByteBufs.readVarLong(this);
    }

    public String readString() {
        int length = (int) readVarLong();
        byte[] bytes = new byte[length];
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public void setOrder(ByteOrder order) {
        throw new UnsupportedOperationException();
    }

    public ByteBuf duplicate() {
        return new ResizableByteBuf(byteList);
    }

    public long readableBytes() {
        return byteList.size();
    }
}
