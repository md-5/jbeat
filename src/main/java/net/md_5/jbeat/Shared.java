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
