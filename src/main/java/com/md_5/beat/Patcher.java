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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.zip.CRC32;

public final class Patcher {

    /**
     * Patch commands used through the patch process.
     */
    private enum Commands {

        SOURCE_READ, TARGET_READ, SOURCE_COPY, TARGET_COPY;
    }
    /**
     * The file header.
     */
    private static final String magicHeader = "BPS1";
    /**
     * UTF-8 charset decoder.
     */
    private static final CharsetDecoder charset = Charset.forName("UTF-8").newDecoder();
    /**
     * File containing the patch.
     */
    private final File patchFile;
    /**
     * Stream to read the patch from.
     */
    private final InspectingInputStream patch;
    /**
     * File containing the source.
     */
    private final File sourceFile;
    /**
     * Stream which is the base of the patch.
     */
    private final RandomAccessFile source;
    /**
     * File containing the target.
     */
    private final File targetFile;
    /**
     * Stream to output the patch to.
     */
    private final RandomAccessFile target;

    public Patcher(File patchFile, File sourceFile, File targetFile) throws FileNotFoundException {
        this.patchFile = patchFile;
        this.patch = new InspectingInputStream(new FileInputStream(patchFile));
        this.sourceFile = sourceFile;
        this.source = new RandomAccessFile(sourceFile, "r");
        this.targetFile = targetFile;
        this.target = new RandomAccessFile(targetFile, "rw");
    }

    /**
     * The meat of the program, patches everything. All logic goes here.
     */
    public void patch() throws IOException {
        try {
            // read header
            byte[] header = new byte[magicHeader.length()];
            patch.read(header);
            // check the header
            for (int i = 0; i < header.length; i++) {
                if (header[i] != magicHeader.charAt(i)) {
                    throw new IOException("Patch file does not contain correct BPS header!");
                }
            }
            // read source size
            long sourceSize = decode(patch);
            // read target size
            long targetSize = decode(patch);
            // read metadata
            String metadata = readString(patch);
            // check that the source file has all the data we need
            if (sourceSize > sourceFile.length()) {
                throw new IOException("Source file smaller than required for patch!");
            }
            // store last offsets
            int sourceOffset = 0, targetOffset = 0;
            // do the actual patching
            while (patch.bytesRead < patchFile.length() - 12) {
                long length = decode(patch);
                Commands mode = Commands.values()[(int) length & 3];
                length = (length >> 2) + 1;
                switch (mode) {
                    case SOURCE_READ:
                        while (length-- != 0) {
                            target.write(source.read());
                        }
                        break;
                    case TARGET_READ:
                        while (length-- != 0) {
                            target.write(patch.read());
                        }
                        break;
                    case SOURCE_COPY:
                    case TARGET_COPY:
                        // start the same
                        long data = decode(patch);
                        long offset = (((data & 1) != 0) ? -1 : 1) * (data >> 1);
                        // descend deeper
                        switch (mode) {
                            case SOURCE_COPY:
                                sourceOffset += offset;
                                while (length-- != 0) {
                                    source.seek(sourceOffset++);
                                    target.write(source.read());
                                }
                                break;
                            case TARGET_COPY:
                                targetOffset += offset;
                                while (length-- != 0) {
                                    target.seek(targetOffset++);
                                    target.write(target.read());
                                }
                                break;
                        }
                        break;
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
            patch.close();
            source.close();
            target.close();
        }
    }

    /**
     * Read a UTF-8 string with variable length number length descriptor. Will
     * return null if there is no data.
     */
    public static String readString(InputStream in) throws IOException {
        int length = (int) decode(in);
        String ret = null;
        if (length != 0) {
            byte[] buf = new byte[length];
            in.read(buf);
            ret = charset.decode(ByteBuffer.wrap(buf)).toString();
        }
        return ret;
    }

    /**
     * Read a big Endian set of bytes from the stream and returns them as a
     * unsigned little Endian integer.
     */
    public static long readInt(InputStream in) throws IOException {
        byte[] b = new byte[4];
        in.read(b);
        return (((b[3] & 0xFF) << 24) + ((b[2] & 0xFF) << 16) + ((b[1] & 0xFF) << 8) + (b[0] & 0xFF)) & 0xFFFFFFFFL;
    }

    public static long checksum(File file, long length) throws IOException {
        CRC32 crc = new CRC32();
        FileInputStream in = new FileInputStream(file);
        int b;
        long read = 0;
        while ((b = in.read()) != -1 && read < length) {
            crc.update(b);
            read++;
        }
        return crc.getValue();
    }

    /**
     * Write a single long to the output stream.
     */
    public static void encode(long data, OutputStream out) throws IOException {
        while (true) {
            byte x = (byte) (data & 0x7f);
            data >>= 7;
            if (data == 0) {
                out.write(0x80 | x);
                break;
            }
            out.write(x);
            data--;
        }
    }

    /**
     * Read a single number from the input stream.
     */
    public static long decode(InputStream in) throws IOException {
        long data = 0, shift = 1;
        while (true) {
            byte x = (byte) in.read();
            data += (x & 0x7f) * shift;
            if ((x & 0x80) != 0x00) {
                break;
            }
            shift <<= 7;
            data += shift;
        }
        return data;
    }

    private class InspectingInputStream extends InputStream {

        private final InputStream wrapped;
        private long bytesRead;

        public InspectingInputStream(InputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int read() throws IOException {
            ++bytesRead;
            return wrapped.read();
        }
    }

    public static void main(String[] args) throws IOException {
        new Patcher(new File("testdata/sourcecopy.bps"), new File("testdata/sourcecopy.source"), new File("out.bin")).patch();
    }
}
