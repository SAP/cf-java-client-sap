package com.sap.cloudfoundry.client.facade.util;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.List;

public class UriUtilTest {

    @Test
    void testEncodeCharsWithOneChar() {
        String encodedString = UriUtil.encodeChars("space,name", List.of(","));
        Assert.assertEquals("space%2Cname", encodedString);
    }

    @Test
    void testEncodeCharsWithMultipleChars() {
        String encodedString = UriUtil.encodeChars("org space,name", List.of(",", " "));
        Assert.assertEquals("org%20space%2Cname", encodedString);
    }
}
