package com.md_5.jbeat;

import static com.md_5.jbeat.Shared.*;
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

        int Granularity = 1;
        while (outputOffset < modified.length()) {
            int sourceLength = 0;
            for (int n = 0; outputOffset + n < Math.min(original.length(), modified.length()); n++) {
                if (source.get(outputOffset + n) != target.get(outputOffset + n)) {
                    break;
                }
                sourceLength++;
            }

            int rleLength = 0;
            for (int n = 1; outputOffset + n < modified.length(); n++) {
                if (target.get(outputOffset) != target.get(outputOffset + n))  {
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
                targetReadLength += Granularity;
                outputOffset += Granularity;
            }
        }
        targetReadFlush();
    }

    private void targetReadFlush() throws IOException {
        if (targetReadLength != 0) {
            encode(out, TARGET_READ | ((targetReadLength - 1) << 2));
            long offset = outputOffset - targetReadLength;
            while (targetReadLength != 0) {
                out.write(target.get((int) offset++));
                targetReadLength--;
            }
        }
    }
}
