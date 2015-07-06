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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import static net.md_5.jbeat.Shared.*;

/**
 * Creates straight binary patches in a linear fashion. No effort is expended
 * applying delta compression.
 */
public final class LinearCreator extends PatchCreator {

    private int targetReadLength, targetRelativeOffset, outputOffset;

    public LinearCreator(File original, File modified, File output) throws FileNotFoundException {
        super(original, modified, output);
    }

    public LinearCreator(File original, File modified, File output, String header) throws FileNotFoundException {
        super(original, modified, output, header);
    }

    @Override
    protected void doPatch() throws IOException {
        while (outputOffset < target.limit()) {
            int sourcePos = 0;
            for (int n = 0; outputOffset + n < Math.min(sourceLength, targetLength); n++) {
                if (source.get(outputOffset + n) != target.get(outputOffset + n)) {
                    break;
                }
                sourcePos++;
            }

            int rleLength = 0;
            for (int n = 1; outputOffset + n < target.limit(); n++) {
                if (target.get(outputOffset) != target.get(outputOffset + n)) {
                    break;
                }
                rleLength++;
            }

            if (rleLength >= 4) {
                //write byte to repeat
                targetReadLength++;
                outputOffset++;
                targetReadFlush();

                //copy starting from repetition byte
                encode(out, TARGET_COPY | ((rleLength - 1) << 2));
                int relativeOffset = (outputOffset - 1) - targetRelativeOffset;
                encode(out, relativeOffset << 1);
                outputOffset += rleLength;
                targetRelativeOffset = outputOffset - 1;
            } else if (sourcePos >= 4) {
                targetReadFlush();
                encode(out, SOURCE_READ | ((sourcePos - 1) << 2));
                outputOffset += sourcePos;
            } else {
                targetReadLength++;
                outputOffset++;
            }
        }
        targetReadFlush();
    }

    /**
     * Write a complete target read statement.
     */
    private void targetReadFlush() throws IOException {
        if (targetReadLength != 0) {
            encode(out, TARGET_READ | ((targetReadLength - 1) << 2));
            int offset = outputOffset - targetReadLength;
            while (targetReadLength != 0) {
                out.write(target.get(offset++));
                targetReadLength--;
            }
        }
    }
}
