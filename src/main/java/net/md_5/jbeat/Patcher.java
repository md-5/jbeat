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

import static net.md_5.jbeat.Shared.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;

public final class Patcher {

    /**
     * The patch.
     */
    private final RandomAccessFile patchFile;
    /**
     * What the patch was generated from.
     */
    private final RandomAccessFile sourceFile;
    /**
     * Where the patched file should go.
     */
    private final RandomAccessFile targetFile;
    /**
     * Reusable per instance crc object.
     */
    private final CRC32 crc = new CRC32();

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
            ByteBuffer patch = patchFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, patchFile.length());
            // check the header
            for (char c : magicHeader) {
                if (patch.get() != c) {
                    throw new IOException("Patch file does not contain correct BPS header!");
                }
            }
            // read source size
            long sourceSize = decode(patch);
            // map as much of the source file as we need
            ByteBuffer source = sourceFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, sourceSize);
            // read target size
            long targetSize = decode(patch);
            // expand the target file
            targetFile.setLength(targetSize);
            // map it into ram
            ByteBuffer target = targetFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, targetSize);
            // read metadata
            String metadata = readString(patch);
            // store last offsets
            int sourceOffset = 0, targetOffset = 0;
            // do the actual patching
            while (patch.position() < patch.limit() - 12) {
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
            if (checksum(source, sourceFile.length(), crc) != sourceChecksum) {
                throw new IOException("Source checksum does not match!");
            }
            // checksum of the target
            long targetChecksum = readInt(patch);
            if (checksum(target, targetFile.length(), crc) != targetChecksum) {
                throw new IOException("Target checksum does not match!");
            }
            // checksum of the patch itself
            long patchChecksum = readInt(patch);
            if (checksum(patch, patchFile.length() - 4, crc) != patchChecksum) {
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
     * return null if there is no data.
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
     * Read a big Endian set of bytes from the stream and returns them as a
     * unsigned integer.
     */
    private long readInt(ByteBuffer in) throws IOException {
        return in.getInt() & 0xFFFFFFFFL;
    }

    /**
     * Read a single number from the input stream.
     */
    private long decode(ByteBuffer in) throws IOException {
        long data = 0, shift = 1;
        while (true) {
            byte x = in.get();
            data += (x & 0x7f) * shift;
            if ((x & 0x80) != 0x00) {
                break;
            }
            shift <<= 7;
            data += shift;
        }
        return data;
    }
}
