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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.md_5.jbeat.util.ByteBuf;
import net.md_5.jbeat.util.ByteBufs;

import static net.md_5.jbeat.Shared.*;

/**
 * beat version 1 compliant binary patcher.
 */
public final class Patcher {

    /**
     * The patch which we will get our instructions from.
     */
    private ByteBuf patch;
    /**
     * The length of the patch bytes
     */
    private final long patchLength;
    /**
     * The clean, unmodified bytes. This must be the same bytes from which the
     * patch was generated.
     */
    private final ByteBuf source;
    /**
     * The location to which the new, patched bytes will be output.
     */
    private final ByteBuf target;

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
    public Patcher(ByteBuf patch, long patchLength, ByteBuf source, ByteBuf target) throws FileNotFoundException {
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
            if (patch.read() != c) {
                throw new IOException("Patch file does not contain correct BPS header!");
            }
        }
        // read source size
        long sourceSize = patch.readVarLong();
        // read target size
        long targetSize = patch.readVarLong();
        // read metadata
        String metadata = readString(patch);
        // store last offsets
        int sourceOffset = 0, targetOffset = 0;
        // do the actual patching
        while (patch.getPosition() < patchLength - 12) {
            long length = patch.readVarLong();
            long mode = length & 3;
            length = (length >> 2) + 1;
            // branch per mode
            if (mode == SOURCE_READ) {
                while (length-- != 0) {
                    target.write(source.read((int) target.getPosition()));
                }
            } else if (mode == TARGET_READ) {
                while (length-- != 0) {
                    target.write(patch.read());
                }
            } else {
                // start the same
                long data = patch.readVarLong();
                long offset = (((data & 1) != 0) ? -1 : 1) * (data >> 1);
                // descend deeper
                if (mode == SOURCE_COPY) {
                    sourceOffset += offset;
                    while (length-- != 0) {
                        target.write(source.read(sourceOffset++));
                    }
                } else {
                    targetOffset += offset;
                    while (length-- != 0) {
                        target.write(target.read(targetOffset++));
                    }
                }
            }
        }
        // flip to little endian mode
        try {
            patch.setOrder(ByteOrder.LITTLE_ENDIAN);
        } catch (UnsupportedOperationException e) {
            patch = ByteBufs.wrap(ByteBuffer.wrap(patch.array()).order(ByteOrder.LITTLE_ENDIAN));
        }
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
    private String readString(ByteBuf in) throws IOException {
        String s = in.readString();
        return s.length() == 0 ? null : s;
    }

    /**
     * Read a set of bytes from a buffer return them as a unsigned integer.
     */
    private long readInt(ByteBuf in) throws IOException {
        return in.readInt() & 0xFFFFFFFFL;
    }
}
