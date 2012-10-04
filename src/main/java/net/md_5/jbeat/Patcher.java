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

import java.io.File;
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
    private final RandomAccessFile patchFile;
    /**
     * The clean, unmodified file. This must be the same file from which the
     * patch was generated.
     */
    private final RandomAccessFile sourceFile;
    /**
     * The location to which the new, patched file will be output.
     */
    private final RandomAccessFile targetFile;

    /**
     * Create a new beat patcher instance. In order to complete the patch
     * process {@link #patch()} method must be called.
     *
     * @param patchFile the beat format patch file
     * @param sourceFile original file from which the patch was created
     * @param targetFile location to which the new, patched file will be output
     * @throws FileNotFoundException when one of the files cannot be opened for
     * read or write access
     */
    public Patcher(File patchFile, File sourceFile, File targetFile) throws FileNotFoundException {
        this.patchFile = new RandomAccessFile(patchFile, "r");
        this.sourceFile = new RandomAccessFile(sourceFile, "r");
        this.targetFile = new RandomAccessFile(targetFile, "rw");
    }

    /**
     * The meat of the program, patches everything. All logic goes here.
     */
    public void patch() throws IOException {
        try {
            // store patch length
            long patchLength = patchFile.length();
            // map patch file into memory
            ByteBuffer patch = patchFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, patchLength);
            // check the header
            for (char c : magicHeader) {
                if (patch.get() != c) {
                    throw new IOException("Patch file does not contain correct BPS header!");
                }
            }
            // read source size
            long sourceSize = decode(patch);
            // map as much of the source file as we need into memory
            ByteBuffer source = sourceFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, sourceSize);
            // read target size
            long targetSize = decode(patch);
            // expand the target file
            targetFile.setLength(targetSize);
            // map a large enough chunk of the target into memory
            ByteBuffer target = targetFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, targetSize);
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
        } finally {
            // close the streams
            patchFile.close();
            sourceFile.close();
            targetFile.close();
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
