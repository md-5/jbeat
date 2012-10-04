/**
 * Copyright (c) 2012, md_5. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * The name of the author may not be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
