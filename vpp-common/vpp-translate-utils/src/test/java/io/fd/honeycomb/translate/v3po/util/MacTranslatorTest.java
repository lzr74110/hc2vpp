package io.fd.honeycomb.translate.v3po.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MacTranslatorTest implements MacTranslator {

    @Test
    public void testParseMac() throws Exception {
        byte[] bytes = parseMac("00:fF:7f:15:5e:A9");
        assertMac(bytes);
    }

    private void assertMac(final byte[] bytes) {
        assertEquals(6, bytes.length);
        assertEquals((byte) 0, bytes[0]);
        assertEquals((byte) 255, bytes[1]);
        assertEquals((byte) 127, bytes[2]);
        assertEquals((byte) 21, bytes[3]);
        assertEquals((byte) 94, bytes[4]);
        assertEquals((byte) 169, bytes[5]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseMacLonger() throws Exception {
        byte[] bytes = parseMac("00:fF:7f:15:5e:A9:88:77");
        assertMac(bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseMacShorter() throws Exception {
        parseMac("00:fF:7f");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseRandomString() throws Exception {
        parseMac("random{}}@$*&*!");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseMacNumberFormatEx() throws Exception {
        parseMac("00:XX:7f:15:5e:77\"");
    }

    @Test
    public void testByteArrayToMacUnseparated() {
        byte[] address = parseMac("aa:bb:cc:dd:ee:ff");

        String converted = byteArrayToMacUnseparated(address);

        assertEquals("aabbccddeeff", converted);
    }

    @Test
    public void testByteArrayToMacSeparated() {
        byte[] address = parseMac("aa:bb:cc:dd:ee:ff");

        String converted = byteArrayToMacSeparated(address);

        assertEquals("aa:bb:cc:dd:ee:ff", converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteArrayToMacUnseparatedIllegal() {
        byteArrayToMacUnseparated(new byte[]{54, 26, 87, 32, 14});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testByteArrayToMacSeparatedIllegal() {
        byteArrayToMacSeparated(new byte[]{54, 26, 87, 32, 14});
    }
}