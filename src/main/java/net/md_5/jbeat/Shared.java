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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.zip.CRC32;

/**
 * Class containing methods common to both beat patch creators and applicators.
 */
final class Shared {

    /**
     * Reads bytes from the source file into the target file.
     */
    static final long SOURCE_READ = 0;
    /**
     * Reads bytes straight from the patch itself and copies them into the
     * target file.
     */
    static final long TARGET_READ = 1;
    /**
     * Copies bytes from the specified offset and length into the target file.
     */
    static final long SOURCE_COPY = 2;
    /**
     * Copies already outputted bytes from the target file to the end of the
     * target file, repeating as necessary.
     */
    static final long TARGET_COPY = 3;
    /**
     * The file header.
     */
    static final char[] magicHeader = new char[]{'B', 'P', 'S', '1'};
    /**
     * beat metadata uses UTF-8 by specification.
     */
    static final Charset charset = Charset.forName("UTF-8");
    /**
     * UTF-8 decoder.
     */
    static final CharsetDecoder decoder = charset.newDecoder();
    /**
     * UTF-8 encoder.
     */
    static final CharsetEncoder encoder = charset.newEncoder();

    /**
     * Creates a crc32 checksum of a ByteBuffer. This method will checksum up to
     * {@code length} bytes from the buffer, starting at the beginning. <p> This
     * method is destructive and will call {@link java.nio.Buffer#rewind()},
     * thus discarding the current mark and position.
     */
    static long checksum(ByteBuffer in, long length) {
        CRC32 crc = new CRC32();
        byte[] back = new byte[(int) length];
        in.rewind();
        in.get(back);
        crc.update(back);
        return crc.getValue();
    }
}
