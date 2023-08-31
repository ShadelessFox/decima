package com.shade.decima.model.util;

import com.shade.util.NotNull;
import com.sun.jna.InvocationMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;

import java.util.Map;

public interface CloseableLibrary extends Library, AutoCloseable {
    @SuppressWarnings("SuspiciousInvocationHandlerImplementation")
    Map<String, Object> LIBRARY_OPTIONS = Map.of(
        Library.OPTION_INVOCATION_MAPPER, (InvocationMapper) (lib, m) -> {
            if (m.getName().equals("close")) {
                return (proxy, method, args) -> {
                    lib.close();
                    return null;
                };
            }
            return null;
        }
    );

    @NotNull
    static <T extends Library> T load(@NotNull String name, @NotNull Class<T> iface) {
        return Native.load(name, iface, LIBRARY_OPTIONS);
    }

    @Override
    void close();
}
