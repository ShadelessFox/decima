package com.shade.platform.ui.utils;

import com.shade.platform.ui.util.UIUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UIUtilsTest {
    @Test
    public void getLabelWithIndexMnemonicTest() {
        assertEquals("&1. test", UIUtils.getLabelWithIndexMnemonic("test", 0));
        assertEquals("&2. test", UIUtils.getLabelWithIndexMnemonic("test", 1));
        assertEquals("&9. test", UIUtils.getLabelWithIndexMnemonic("test", 8));
        assertEquals("&0. test", UIUtils.getLabelWithIndexMnemonic("test", 9));
        assertEquals("&A. test", UIUtils.getLabelWithIndexMnemonic("test", 10));
        assertEquals("&Z. test", UIUtils.getLabelWithIndexMnemonic("test", 35));
        assertEquals("test", UIUtils.getLabelWithIndexMnemonic("test", 36));
    }
}
