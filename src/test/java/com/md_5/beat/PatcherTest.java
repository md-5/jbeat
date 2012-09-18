package com.md_5.beat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class PatcherTest {

    /**
     * Map of examples, containing an integer key and an encoded array value.
     */
    private Map<Integer, Integer[]> examples = new LinkedHashMap<Integer, Integer[]>() {
        {
            put(0, new Integer[]{0x80});
            put(1, new Integer[]{0x81});
            put(127, new Integer[]{0xFF});
            put(128, new Integer[]{0x00, 0x80});
            put(129, new Integer[]{0x01, 0x80});
            put(255, new Integer[]{0x7F, 0x80});
            put(256, new Integer[]{0x00, 0x81});
        }
    };

    /**
     * Test that plain integers encode to the correct bytes.
     */
    @Test
    public void numberEncode() throws IOException {
        for (Map.Entry<Integer, Integer[]> pair : examples.entrySet()) {
            // value to encode
            int value = pair.getKey();
            // what we should get
            Integer[] expected = pair.getValue();
            // encode
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Patcher.encode(value, out);
            // get the encoded value
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            for (int i : expected) {
                assert i == in.read();
            }
        }
    }

    /**
     * Test that bytes decode to the correct integers.
     */
    @Test
    public void numberDecode() throws IOException {
        for (Map.Entry<Integer, Integer[]> pair : examples.entrySet()) {
            // value to decode
            byte[] encoded = new byte[pair.getValue().length];
            // manually copy array
            for (int i = 0; i < encoded.length; i++) {
                encoded[i] = pair.getValue()[i].byteValue();
            }
            // what we should get
            int expected = pair.getKey();
            // decode
            ByteArrayInputStream in = new ByteArrayInputStream(encoded);
            assert expected == Patcher.decode(in);
        }
    }

    /**
     * Tests big to small Endian conversion.
     */
    @Test
    public void endianConvert() throws IOException {
        assert 0x81000000 == Patcher.readInt(new ByteArrayInputStream(new byte[]{(byte) 00, (byte) 00, (byte) 00, (byte) 0x81}));
    }
}
