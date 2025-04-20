package com.shade.decima.ui.data.viewer.shader.com;

public final class COMException extends RuntimeException {
    private final int result;

    public COMException(int result) {
        this.result = result;
    }

    public static void check(int rc) {
        if (rc != 0) {
            throw new COMException(rc);
        }
    }

    @Override
    public String getMessage() {
        return "0x%08x".formatted(result);
    }
}
