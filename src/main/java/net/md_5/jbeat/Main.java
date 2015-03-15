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

import java.io.File;
import java.io.FileNotFoundException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

import lombok.*;

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
        try {
            patchCreator = linear ? new LinearCreator(first, second, patch) : null;
        } catch (FileNotFoundException e) {
            System.err.println("Unable to open file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        try {
            patchCreator.create();
        } catch (Exception e) {
            System.err.println("Unable to create patch of " + first.getName() + " and " + second.getName());
            e.printStackTrace();
            System.exit(1);
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
        
        try {
            Patcher patcher = new Patcher(patch, in, out);
            patcher.patch();
        } catch (Exception e) {
            System.err.println("Unable to patch file " + in.getName());
            e.printStackTrace();
            System.exit(1);
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
