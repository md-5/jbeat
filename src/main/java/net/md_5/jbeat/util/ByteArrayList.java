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

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class ByteArrayList implements ByteList {

    private final ReadWriteLock bytesLock = new ReentrantReadWriteLock();
    private byte[] bytes;
    private int length;

    private static final int DEFAULT_CAPACITY = 20;

    public ByteArrayList() {
        this(DEFAULT_CAPACITY);
    }

    public ByteArrayList(int initialCapacity) {
        bytes = new byte[initialCapacity];
    }

    public void add(byte b) {
        grow(length + 1);
        bytesLock.readLock().lock();
        try {
            bytes[length] = b;
        } finally {
            bytesLock.readLock().unlock();
        }
        length++;
    }

    public byte get(int index) {
        return bytes[index];
    }

    public void addAll(byte[] bytes) {
        addAll(bytes, 0, bytes.length);
    }

    public void addAll(final byte[] bytes, int start, final int end) {
        grow(length + bytes.length);
        for (; start < end; start++) {
            add(bytes[start]);
        }
    }

    public static final int MINIMUM_GROW = 20;

    private void grow(int newLength) {
        if (length >= newLength) return;
        bytes = Arrays.copyOf(bytes, newLength + MINIMUM_GROW);
    }

    public byte[] toArray() {
        return Arrays.copyOf(bytes, length);
    }

    public int size() {
        return length;
    }
}
