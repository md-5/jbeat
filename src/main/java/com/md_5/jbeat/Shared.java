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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.zip.CRC32;

class Shared {

    /**
     * Patch commands used through the patch process.
     */
    static final long SOURCE_READ = 0;
    static final long TARGET_READ = 1;
    static final long SOURCE_COPY = 2;
    static final long TARGET_COPY = 3;
    /**
     * The file header.
     */
    static final char[] magicHeader = new char[]{'B', 'P', 'S', '1'};
    /**
     * UTF-8 charset decoder.
     */
    static final Charset charset = Charset.forName("UTF-8");
    static final CharsetDecoder decoder = charset.newDecoder();
    static final CharsetEncoder encoder = charset.newEncoder();

    /**
     * Checksums a byte buffer uses a reusable crc32 instance. This method will
     * checksum up to {@code length} bytes from the buffer. It is destructive
     * and will call {@link ByteBuffer.reset()}
     */
    static long checksum(ByteBuffer in, long length, CRC32 crc) throws IOException {
        byte[] back = new byte[(int) length];
        in.rewind();
        in.get(back);
        crc.reset();
        crc.update(back);
        return crc.getValue();
    }
}
