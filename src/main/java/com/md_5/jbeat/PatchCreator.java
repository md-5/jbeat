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
package com.md_5.jbeat;

import static com.md_5.jbeat.Shared.*;
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
import java.util.zip.CRC32;

abstract class PatchCreator {

    protected final RandomAccessFile original;
    protected final RandomAccessFile modified;
    protected ByteBuffer source;
    protected ByteBuffer target;
    protected final File output;
    protected final OutputStream out;
    private final String header;
    private final CRC32 crc = new CRC32();

    protected PatchCreator(File original, File modified, File output, String header) throws FileNotFoundException {
        this.original = new RandomAccessFile(original, "r");
        this.modified = new RandomAccessFile(modified, "r");
        this.output = output;
        this.out = new BufferedOutputStream(new FileOutputStream(output));
        this.header = header;
    }

    protected PatchCreator(File original, File modified, File output) throws FileNotFoundException {
        this(original, modified, output, null);
    }

    public void create() throws IOException {
        try {
            // map the files
            source = original.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, original.length());
            target = modified.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, modified.length());
            // write header
            for (char c : magicHeader) {
                out.write(c);
            }
            // write original size
            encode(out, source.limit());
            // write modified size
            encode(out, target.limit());
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
            writeIntLE(out, (int) checksum(source, source.limit(), crc));
            // write target checksum
            writeIntLE(out, (int) checksum(target, target.limit(), crc));
            // map ourselves to ram
            out.flush();
            ByteBuffer self = new RandomAccessFile(output, "rw").getChannel().map(FileChannel.MapMode.READ_ONLY, 0, output.length());
            // write self checksum
            writeIntLE(out, (int) checksum(self, self.limit(), crc));
        } finally {
            // close the streams
            original.close();
            modified.close();
            out.close();
        }
    }

    private void writeIntLE(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

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

    protected abstract void doPatch() throws IOException;
}
