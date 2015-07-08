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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import net.md_5.jbeat.util.ByteBuf;

import static net.md_5.jbeat.Shared.*;

/**
 * Patch creator base. Handles the header and footer when making a patch.
 */
abstract class PatchCreator {

    /**
     * The source mapped into memory.
     */
    protected ByteBuf source;
    /**
     * Length of the source.
     */
    protected long sourceLength;
    /**
     * The target mapped into memory.
     */
    protected ByteBuf target;
    /**
     * Length of the target.
     */
    protected long targetLength;
    /**
     * The location to which the patch will be generated.
     */
    protected final ByteBuf out;
    /**
     * UTF-8, optional patch header.
     */
    private final String header;

    /**
     * Creates a new beat patch creator instance. In order to create and output
     * the patch the {@link #create()} method must be called.
     *
     * @param source the original bytes to generate the patch from
     * @param sourceLength, the number of bytes in the source
     * @param modified the modified bytes which has been changed from the original
     * @param modifiedLength the number of modified bytes
     * @param output location to which the patch will be output
     * @param header to be used as beat metadata
     */
    protected PatchCreator(ByteBuf source, long sourceLength, ByteBuf modified, long modifiedLength, ByteBuf output, String header) {
        this.source = source;
        this.sourceLength = sourceLength;
        this.target = modified;
        this.targetLength = modifiedLength;
        this.out = output;
        this.header = header;
    }

    protected PatchCreator(ByteBuf source, long sourceLength, ByteBuf modified, long modifiedLength, ByteBuf output) {
        this(source, sourceLength, modified, modifiedLength, output, null);
    }

    /**
     * Creates a beat version 1 format binary patch of the two files specified
     * in the constructor. This method will header the file with beat
     * information, delegate binary differencing to the specific patch style
     * implementation, and then finish the patch with the various checksums
     * before writing to disk.
     */
    public void create() throws IOException {
        // write header
        for (char c : magicHeader) {
            out.write((byte) c);
        }
        // write original size
        encode(out, sourceLength);
        // write modified size
        encode(out, targetLength);
        // write header length
        int headerLength = (header == null) ? 0 : header.length();
        encode(out, headerLength);
        // write the header
        if (header != null) {
            ByteBuffer encoded = encoder.encode(CharBuffer.wrap(header));
            out.write(encoded.array(), encoded.arrayOffset(), encoded.limit());
        }
        // do the actual patch
        doPatch();
        // write original checksum
        writeIntLE(out, (int) checksum(source, sourceLength));
        // write target checksum
        writeIntLE(out, (int) checksum(target, targetLength));
        // store patch length
        long outLength = out.getPosition();
        // write self checksum
        ByteBuf duplicated = out.duplicate();
        duplicated.reset();
        writeIntLE(out, (int) checksum(duplicated, outLength));
    }

    /**
     * Writes and integer to the specified output stream in it's little Endian
     * form. This method does not & with 0xFF and should not need to.
     */
    private void writeIntLE(ByteBuf out, int value) throws IOException {
        out.write((byte) value);
        out.write((byte) (value >> 8));
        out.write((byte) (value >> 16));
        out.write((byte) (value >> 24));
    }

    /**
     * Encode a single number as into it's variable length form and write it to
     * the output stream.
     */
    protected final void encode(ByteBuf out, long data) throws IOException {
        while (true) {
            long x = data & 0x7f;
            data >>= 7;
            if (data == 0) {
                out.write((byte) (0x80 | x));
                break;
            }
            out.write((byte) x);
            data--;
        }
    }

    /**
     * Method which the patch implementation must overwrite to generate the
     * binary differences for the patch.
     */
    protected abstract void doPatch() throws IOException;
}
