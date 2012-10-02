package com.md_5.jbeat;

import static com.md_5.jbeat.Shared.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.CRC32;

abstract class PatchCreator {

    protected final RandomAccessFile original;
    protected final RandomAccessFile modified;
    protected final RandomAccessFile output;
    protected ByteBuffer source;
    protected ByteBuffer target;
    protected ByteBuffer out;
    private final String header;
    private final CRC32 crc = new CRC32();

    protected PatchCreator(File original, File modified, File output, String header) throws FileNotFoundException {
        this.original = new RandomAccessFile(original, "r");
        this.modified = new RandomAccessFile(modified, "r");
        this.output = new RandomAccessFile(output, "rw");
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
            out = output.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, target.limit());
            // write header
            for (char c : magicHeader) {
                out.put((byte) c);
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
                out.put(encoded.array(), encoded.arrayOffset(), encoded.limit());
            }
            // do the actual patch
            doPatch();
            // flip to little endian mode
            out.order(ByteOrder.LITTLE_ENDIAN);
            // write original checksum
            out.putInt((int) checksum(source, source.limit(), crc));
            // write target checksum
            out.putInt((int) checksum(target, target.limit(), crc));
            // write self checksum
            out.putInt((int) checksum(out, out.position(), crc));
            // truncate the file
            output.setLength(out.position());
        } finally {
            // close the streams
            original.close();
            modified.close();
            output.close();
        }
    }

    protected final void encode(ByteBuffer out, long data) throws IOException {
        while (true) {
            long x = data & 0x7F;
            data >>= 7;
            if (data == 0) {
                out.put((byte) (0x80 | x));
                break;
            }
            out.put((byte) x);
            data--;
        }
    }

    protected abstract void doPatch() throws IOException;
}
