package com.shade.util.compression;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class LZ4Decompressor extends Decompressor {
    @Override
    public void decompress(ByteBuffer src, ByteBuffer dst) throws IOException {
        // Special case
        if (dst.remaining() == 0) {
            if (src.remaining() != 1 || src.get() != 0) {
                throw new IOException("Invalid empty block");
            }
            return /*0*/;
        }

        src.order(ByteOrder.LITTLE_ENDIAN);
        while (true) {
            int token = src.get();

            // Get the literal len
            int literalLength = (token >>> 4) & 0x0F;
            if (literalLength != 0) {
                if (literalLength == 15) {
                    int temp;
                    do {
                        temp = Byte.toUnsignedInt(src.get());
                        literalLength += temp;
                    } while (temp == 255);
                }

                // Copy the literal over
                copyLiteral(src, dst, literalLength);
            }

            // End of input check
            if (!src.hasRemaining()) {
                return /*dstPos - targetOffset*/;
            }

            // Get the match position, can't start before the output start
            int offset = Short.toUnsignedInt(src.getShort());

            // Get the match length
            int matchLength = token & 0x0F;
            if (matchLength == 15) {
                int temp;
                do {
                    temp = Byte.toUnsignedInt(src.get());
                    matchLength += temp;
                } while (temp == 255);
            }
            matchLength += 4;

            // Can't copy past the end of the output
            copyReference(dst, offset, matchLength);
        }
    }

    void copy(ByteBuffer src, ByteBuffer dst, int length) {
        dst.put(dst.position(), src, src.position(), length);
        dst.position(dst.position() + length);
        src.position(src.position() + length);
    }

    void copyLiteral(ByteBuffer src, ByteBuffer dst, int len) throws IOException {
        if (len <= 0 || src.remaining() < len || dst.remaining() < len) {
            throw new IOException("Invalid literal");
        }
        copy(src, dst, len);
    }

    void copyReference(ByteBuffer dst, int offset, int length) throws IOException {
        if (offset <= 0 || dst.position() - offset < 0 || length > dst.remaining()) {
            throw new IOException("Invalid match");
        }
        if (offset == 1) {
            var b = dst.get(dst.position() - 1);
            for (var i = 0; i < length; i++) {
                dst.put(b);
            }
        } else if (offset >= length) {
            dst.put(dst.slice(dst.position() - offset, length));
        } else {
            for (int i = 0, pos = dst.position() - offset; i < length; i++) {
                dst.put(dst.get(pos + i));
            }
        }
    }
}
