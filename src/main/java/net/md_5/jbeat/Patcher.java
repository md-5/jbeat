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
package net.md_5.jbeat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import static net.md_5.jbeat.Shared.*;

/**
 * beat version 1 compliant binary patcher.
 */
public final class Patcher {

    /**
     * The patch which we will get our instructions from.
     */
    private final ByteBuffer patch;
    /**
     * The length of the patch bytes
     */
    private final long patchLength;
    /**
     * The clean, unmodified bytes. This must be the same bytes from which the
     * patch was generated.
     */
    private final ByteBuffer source;
    /**
     * The location to which the new, patched bytes will be output.
     */
    private final ByteBuffer target;

    /**
     * Create a new beat patcher instance. In order to complete the patch
     * process {@link #patch()} method must be called.
     *
     * @param patch the beat format patch
     * @param patchLength the length of the patch
     * @param source original from which the patch was created
     * @param target location to which the new, patched bytes will be output
     *
     * @throws FileNotFoundException when one of the files cannot be opened for
     * read or write access
     */
    public Patcher(ByteBuffer patch, long patchLength, ByteBuffer source, ByteBuffer target) throws FileNotFoundException {
        this.patch = patch;
        this.patchLength = patchLength;
        this.source = source;
        this.target = target;
    }

    /**
     * The meat of the program, patches everything. All logic goes here.
     */
    public void patch() throws IOException {
        // check the header
        for (char c : magicHeader) {
            if (patch.get() != c) {
                throw new IOException("Patch file does not contain correct BPS header!");
            }
        }
        // read source size
        long sourceSize = decode(patch);
        // read target size
        long targetSize = decode(patch);
        // read metadata
        String metadata = readString(patch);
        // store last offsets
        int sourceOffset = 0, targetOffset = 0;
        // do the actual patching
        while (patch.position() < patchLength - 12) {
            long length = decode(patch);
            long mode = length & 3;
            length = (length >> 2) + 1;
            // branch per mode
            if (mode == SOURCE_READ) {
                while (length-- != 0) {
                    target.put(source.get(target.position()));
                }
            } else if (mode == TARGET_READ) {
                while (length-- != 0) {
                    target.put(patch.get());
                }
            } else {
                // start the same
                long data = decode(patch);
                long offset = (((data & 1) != 0) ? -1 : 1) * (data >> 1);
                // descend deeper
                if (mode == SOURCE_COPY) {
                    sourceOffset += offset;
                    while (length-- != 0) {
                        target.put(source.get(sourceOffset++));
                    }
                } else {
                    targetOffset += offset;
                    while (length-- != 0) {
                        target.put(target.get(targetOffset++));
                    }
                }
            }
        }
        // flip to little endian mode
        patch.order(ByteOrder.LITTLE_ENDIAN);
        // checksum of the source
        long sourceChecksum = readInt(patch);
        if (checksum(source, sourceSize) != sourceChecksum) {
            throw new IOException("Source checksum does not match!");
        }
        // checksum of the target
        long targetChecksum = readInt(patch);
        if (checksum(target, targetSize) != targetChecksum) {
            throw new IOException("Target checksum does not match!");
        }
        // checksum of the patch itself
        long patchChecksum = readInt(patch);
        if (checksum(patch, patchLength - 4) != patchChecksum) {
            throw new IOException("Patch checksum does not match!");
        }
    }

    /**
     * Read a UTF-8 string with variable length number length descriptor. Will
     * return null if there is no data read, or the string is of 0 length.
     */
    private String readString(ByteBuffer in) throws IOException {
        int length = (int) decode(in);
        String ret = null;
        if (length != 0) {
            int limit = in.limit();
            in.limit(in.position() + length);
            ret = charset.decode(in).toString();
            in.limit(limit);
        }
        return ret;
    }

    /**
     * Read a set of bytes from a buffer return them as a unsigned integer.
     */
    private long readInt(ByteBuffer in) throws IOException {
        return in.getInt() & 0xFFFFFFFFL;
    }

    /**
     * Read a single variable length number from the input stream.
     */
    private long decode(ByteBuffer in) throws IOException {
        long data = 0, shift = 1;
        while (true) {
            byte x = in.get();
            data += (x & 0x7F) * shift;
            if ((x & 0x80) != 0x00) {
                break;
            }
            shift <<= 7;
            data += shift;
        }
        return data;
    }
}
