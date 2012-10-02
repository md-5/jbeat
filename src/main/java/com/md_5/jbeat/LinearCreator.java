package com.md_5.jbeat;

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

    @Override
    protected void doPatch() throws IOException {
    //    throw new UnsupportedOperationException("Not supported yet.");
    }
}
