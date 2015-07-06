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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import static net.md_5.jbeat.Shared.*;

/**
 * Patch creator base. Handles the header and footer when making a patch.
 */
abstract class PatchCreator {

    /**
     * The clean, unmodified file.
     */
    protected final RandomAccessFile sourceFile;
    /**
     * The source file mapped into memory.
     */
    protected ByteBuffer source;
    /**
     * Length of the source file.
     */
    protected long sourceLength;
    /**
     * The modified file which we will difference with the source file.
     */
    protected final RandomAccessFile targetFile;
    /**
     * The target file mapped into memory.
     */
    protected ByteBuffer target;
    /**
     * Length of the target file.
     */
    protected long targetLength;
    /**
     * The location to which the patch will be generated.
     */
    protected final File outFile;
    /**
     * Stream to the patch output.
     */
    protected final OutputStream out;
    /**
     * UTF-8, optional patch header.
     */
    private final String header;

    /**
     * Creates a new beat patch creator instance. In order to create and output
     * the patch the {@link #create()} method must be called.
     *
     * @param original file, which the patch applicator will have access to
     * @param modified file which has been changed from the original
     * @param output location to which the patch will be output
     * @param header to be used as beat metadata
     * @throws FileNotFoundException when one of the files cannot be opened for
     * read or write access
     */
    protected PatchCreator(File original, File modified, File output, String header) throws FileNotFoundException {
        this.sourceFile = new RandomAccessFile(original, "r");
        this.targetFile = new RandomAccessFile(modified, "r");
        this.out = new BufferedOutputStream(new FileOutputStream(output));
        this.outFile = output;
        this.header = header;
    }

    protected PatchCreator(File original, File modified, File output) throws FileNotFoundException {
        this(original, modified, output, null);
    }

    /**
     * Creates a beat version 1 format binary patch of the two files specified
     * in the contrstructor. This method will header the file with beat
     * information, delegate binary differencing to the specific patch style
     * implementation, and then finish the patch with the various checksums
     * before writing to disk.
     */
    public void create() throws IOException {
        try {
            // store file lengths
            sourceLength = sourceFile.length();
            targetLength = targetFile.length();
            // map the files
            source = sourceFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, sourceLength);
            target = targetFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, targetLength);
            // write header
            for (char c : magicHeader) {
                out.write(c);
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
            // map ourselves to ram
            out.flush();
            // store patch length
            long outLength = outFile.length();
            ByteBuffer self = new RandomAccessFile(outFile, "rw").getChannel().map(FileChannel.MapMode.READ_ONLY, 0, outLength);
            // write self checksum
            writeIntLE(out, (int) checksum(self, outLength));
        } finally {
            // close the streams
            sourceFile.close();
            targetFile.close();
            out.close();
        }
    }

    /**
     * Writes and integer to the specified output stream in it's little Endian
     * form. This method does not & with 0xFF and should not need to.
     */
    private void writeIntLE(OutputStream out, int value) throws IOException {
        out.write(value);
        out.write(value >> 8);
        out.write(value >> 16);
        out.write(value >> 24);
    }

    /**
     * Encode a single number as into it's variable length form and write it to
     * the output stream.
     */
    protected final void encode(OutputStream out, long data) throws IOException {
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
