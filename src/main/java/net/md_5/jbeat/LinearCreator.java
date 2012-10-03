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

public final class LinearCreator extends PatchCreator {

    public LinearCreator(File original, File modified, File output) throws FileNotFoundException {
        super(original, modified, output);
    }

    public LinearCreator(File original, File modified, File output, String header) throws FileNotFoundException {
        super(original, modified, output, header);
    }
    int targetReadLength = 0;
    int targetRelativeOffset = 0, outputOffset = 0;

    @Override
    protected void doPatch() throws IOException {
        while (outputOffset < target.limit()) {
            int sourceLength = 0;
            for (int n = 0; outputOffset + n < Math.min(source.limit(), target.limit()); n++) {
                if (source.get(outputOffset + n) != target.get(outputOffset + n)) {
                    break;
                }
                sourceLength++;
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
            } else if (sourceLength >= 4) {
                targetReadFlush();
                encode(out, SOURCE_READ | ((sourceLength - 1) << 2));
                outputOffset += sourceLength;
            } else {
                targetReadLength++;
                outputOffset++;
            }
        }
        targetReadFlush();
    }

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
