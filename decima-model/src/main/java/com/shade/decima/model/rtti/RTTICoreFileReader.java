package com.shade.decima.model.rtti;

import com.shade.decima.model.archive.ArchiveFile;
import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public interface RTTICoreFileReader {
    @NotNull
    RTTICoreFile read(@NotNull InputStream is, @NotNull ErrorHandlingStrategy errorHandlingStrategy) throws IOException;

    @NotNull
    byte[] write(@NotNull RTTICoreFile file);

    @NotNull
    default RTTICoreFile read(@NotNull ArchiveFile file, @NotNull ErrorHandlingStrategy errorHandlingStrategy) throws IOException {
        try (InputStream is = file.newInputStream()) {
            return read(is, errorHandlingStrategy);
        }
    }

    interface ErrorHandlingStrategy {
        void handle(@NotNull Exception e);
    }

    class LoggingErrorHandlingStrategy implements ErrorHandlingStrategy {
        private static final Logger log = LoggerFactory.getLogger(LoggingErrorHandlingStrategy.class);
        private static final LoggingErrorHandlingStrategy INSTANCE = new LoggingErrorHandlingStrategy();

        @NotNull
        public static LoggingErrorHandlingStrategy getInstance() {
            return INSTANCE;
        }

        @Override
        public void handle(@NotNull Exception e) {
            log.error(e.getMessage());
        }
    }

    class ThrowingErrorHandlingStrategy implements ErrorHandlingStrategy {
        private static final ErrorHandlingStrategy INSTANCE = new LoggingErrorHandlingStrategy();

        @NotNull
        public static ErrorHandlingStrategy getInstance() {
            return INSTANCE;
        }

        @Override
        public void handle(@NotNull Exception e) {
            IOUtils.sneakyThrow(e);
        }
    }
}
