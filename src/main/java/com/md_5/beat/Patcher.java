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
package com.md_5.beat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.zip.CRC32;

public final class Patcher {

    /**
     * Patch commands used through the patch process.
     */
    private static final long SOURCE_READ = 0;
    private static final long TARGET_READ = 1;
    private static final long SOURCE_COPY = 2;
    private static final long TARGET_COPY = 3;
    /**
     * The file header.
     */
    private static final String magicHeader = "BPS1";
    /**
     * UTF-8 charset decoder.
     */
    private static final CharsetDecoder charset = Charset.forName("UTF-8").newDecoder();
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
     * Buffer for byte read operations.
     */
    private final ByteBuffer buf = ByteBuffer.allocateDirect(1);

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
            FileChannel patch = patchFile.getChannel();
            FileChannel source = sourceFile.getChannel();
            // read header
            ByteBuffer header = ByteBuffer.allocateDirect(magicHeader.length());
            patch.read(header);
            header.flip();
            // check the header
            for (char c : magicHeader.toCharArray()) {
                if (header.get() != c) {
                    throw new IOException("Patch file does not contain correct BPS header!");
                }
            }
            // read source size
            long sourceSize = decode(patch);
            // read target size
            long targetSize = decode(patch);
            targetFile.setLength(targetSize);
            MappedByteBuffer target = targetFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, targetSize);
            // read metadata
            String metadata = readString(patch);
            // store last offsets
            int sourceOffset = 0, targetOffset = 0;
            // do the actual patching
            while (patch.position() < patch.size() - 12) {
                long length = decode(patch);
                long mode = length & 3;
                length = (length >> 2) + 1;
                // branch per mode
                if (mode == SOURCE_READ) {
                    while (length-- != 0) {
                        source.position(target.position());
                        target.put(readByte(source));
                    }
                } else if (mode == TARGET_READ) {
                    while (length-- != 0) {
                        target.put(readByte(patch));
                    }
                } else {
                    // start the same
                    long data = decode(patch);
                    long offset = (((data & 1) != 0) ? -1 : 1) * (data >> 1);
                    // descend deeper
                    if (mode == SOURCE_COPY) {
                        sourceOffset += offset;
                        while (length-- != 0) {
                            source.position(sourceOffset++);
                            target.put(readByte(source));
                        }
                    } else {
                        targetOffset += offset;
                        while (length-- != 0) {
                            target.position(targetOffset++);
                            target.put(target.get());
                        }
                    }
                }
            }
            // checksum of the source
            long sourceChecksum = readInt(patch);
            if (checksum(sourceFile, sourceFile.length()) != sourceChecksum) {
                throw new IOException("Source checksums do not match!");
            }
            // checksum of the target
            long targetChecksum = readInt(patch);
            if (checksum(targetFile, targetFile.length()) != targetChecksum) {
                throw new IOException("Target checksums do now match!");
            }
            // checksum of the patch itself
            long patchChecksum = readInt(patch);
            if (checksum(patchFile, patchFile.length() - 4) != patchChecksum) {
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
    private String readString(ReadableByteChannel in) throws IOException {
        int length = (int) decode(in);
        String ret = null;
        if (length != 0) {
            ByteBuffer buf = ByteBuffer.allocateDirect(length);
            in.read(buf);
            ret = charset.decode(buf).toString();
        }
        return ret;
    }

    /**
     * Read a big Endian set of bytes from the stream and returns them as a
     * unsigned little Endian integer.
     */
    public static long readInt(ReadableByteChannel in) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(4);
        in.read(b);
        b.flip();
        return b.order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
    }

    public long checksum(RandomAccessFile in, long length) throws IOException {
        CRC32 crc = new CRC32();
        MappedByteBuffer map = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);
        byte[] back = new byte[(int) length];
        map.get(back);
        crc.update(back);
        return crc.getValue();
    }

    /**
     * Read a single number from the input stream.
     */
    private long decode(ReadableByteChannel in) throws IOException {
        long data = 0, shift = 1;
        while (true) {
            byte x = readByte(in);
            data += (x & 0x7f) * shift;
            if ((x & 0x80) != 0x00) {
                break;
            }
            shift <<= 7;
            data += shift;
        }
        return data;
    }

    private void writeByte(int b, WritableByteChannel out) throws IOException {
        buf.rewind();
        buf.put((byte) b);
        out.write(buf);
    }

    private byte readByte(ReadableByteChannel in) throws IOException {
        buf.rewind();
        in.read(buf);
        buf.flip();
        return buf.get();
    }

    public static void main(String[] args) throws IOException {
        File patch = new File("Patch.bps");
        File source = new File("Original.sfc");
        File target = new File("out.bin");

        new Patcher(patch, source, target).patch();
    }
}
