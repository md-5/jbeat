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

import lombok.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel.MapMode;

import net.md_5.jbeat.util.ByteBuf;
import net.md_5.jbeat.util.ByteBufs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

public class Main {

    public static void main(String[] rawArgs) {
        JBeatOptions options = new JBeatOptions();
        new JCommander(options, rawArgs);
        if (options.getFirstFile() != null && options.getSecondFile() != null) {
            createPatch(options.getFirstFile(), options.getSecondFile(), options.getPatchOut(), true);
        } else if (options.getInFile() != null && options.getOutFile() != null && options.getPatchIn() != null) {
            patch(options.getInFile(), options.getPatchIn(), options.getOutFile());
        } else {
            System.err.println("You must either specify --first-file and --second-file to generate a patch");
            System.err.println("Or --in-file --patch-in and --out-file to patch a file");
            System.exit(1);
        }
    }

    public static void createPatch(File first, File second, File patch, boolean linear) {
        if (patch.exists()) {
            System.err.println("The patch file " + patch.getName() + " exists.");
            System.err.println("Please delete it before running this program");
            System.exit(1);
        }
        if (!linear) throw new UnsupportedOperationException("Only linear patch creation is supported");
        createFile(patch);
        PatchCreator patchCreator = null;
        RandomAccessFile firstRandom = null, secondRandom = null, output = null;
        try {
            firstRandom = new RandomAccessFile(first, "r");
            secondRandom = new RandomAccessFile(second, "r");
            output = new RandomAccessFile(patch, "rw");
        } catch (FileNotFoundException e) {
            System.err.println("Unable to open file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        try {
            long firstLength = firstRandom.length();
            long secondLength = secondRandom.length();
            ByteBuf firstBuffer = ByteBufs.wrap(firstRandom.getChannel().map(MapMode.READ_ONLY, 0, firstLength));
            ByteBuf secondBuffer = ByteBufs.wrap(secondRandom.getChannel().map(MapMode.READ_ONLY, 0, secondLength));
            ByteBuf outputBuffer = ByteBufs.create();
            patchCreator = linear ? new LinearCreator(firstBuffer, firstLength, secondBuffer, secondLength, outputBuffer) : null;
            output.write(outputBuffer.array());
            output.close();
            patchCreator.create();
        } catch (Exception e) {
            System.err.println("Unable to create patch of " + first.getName() + " and " + second.getName());
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                if (firstRandom != null) firstRandom.close();
                if (secondRandom != null) secondRandom.close();
                ;
            } catch (IOException e) {
                System.err.println("Unable to close files");
            }
        }
        System.out.println("Successfuly created patch of " + first.getName() + " and " + second.getName() + " in " + patch.getName());
    }

    public static void patch(File in, File patch, File out) {
        if (out.exists()) {
            System.err.println("The output file " + out.getName() + " exists.");
            System.err.println("Please delete it before running this program");
            System.exit(1);
        }
        createFile(out);

        RandomAccessFile inRandom = null, patchRandom = null, outRandom = null;
        try {
            inRandom = new RandomAccessFile(in, "r");
            patchRandom = new RandomAccessFile(patch, "r");
            outRandom = new RandomAccessFile(out, "rw");
        } catch (FileNotFoundException ex) {
            System.err.println("No such file: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
        try {
            ByteBuf inBytes = ByteBufs.wrap(inRandom.getChannel().map(MapMode.READ_ONLY, 0, inRandom.length()));
            ByteBuf patchBytes = ByteBufs.wrap(patchRandom.getChannel().map(MapMode.READ_ONLY, 0, patchRandom.length()));
            ByteBuf outBytes = ByteBufs.create((int) inRandom.length());
            Patcher patcher = new Patcher(patchBytes, patchRandom.length(), inBytes, outBytes);
            patcher.patch();
            outRandom.write(outBytes.array());
        } catch (Exception e) {
            System.err.println("Unable to patch file " + in.getName());
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                if (inRandom != null) inRandom.close();
                if (patchRandom != null) patchRandom.close();
                if (outRandom != null) outRandom.close();
            } catch (IOException e) {
                System.err.println("Unable to close: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        System.out.println("Successfuly Patched: " + in.getName() + " with " + patch.getName());
    }

    private static void createFile(File f) {
        try {
            f.createNewFile();
        } catch (Exception e) {
            System.err.println("Unable to create file:" + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Getter
    public static class JBeatOptions {

        @Parameter(names = {"-a", "--first-file"}, description = "Original file for generating patch", converter = FileConverter.class)
        private File firstFile;

        @Parameter(names = {"-b", "--second-file"}, description = "Changed file for generating patch", converter = FileConverter.class)
        private File secondFile;

        @Parameter(names = {"--patch-out"}, description = "The file to put the generated patch in", converter = FileConverter.class)
        private File patchOut = new File("output.bps");

        @Parameter(names = {"-i", "--in-file"}, description = "Input file to patch", converter = FileConverter.class)
        private File inFile;

        @Parameter(names = {"-o", "--out-file"}, description = "Write patched file here", converter = FileConverter.class)
        private File outFile;

        @Parameter(names = {"-p", "--patch-in"}, description = "Patch to use to patch in file", converter = FileConverter.class)
        private File patchIn;
        
        /* JBeat only supports linear patches
        @Paramater(names = {"-l", "--linear-patch"}, description = "Create binary patches in a linear fashion without delta compression.")
        private boolean linearPatch = true;
        */
    }
}
