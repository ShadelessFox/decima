package com.shade.decima.model.rtti.types.java;

import com.shade.decima.model.base.GameType;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;

public record HwShader(int unk0, int unk1, int unk2, @NotNull Entry[] programs, @NotNull byte[] rootSignature) {
    @NotNull
    public static HwShader read(@NotNull ByteBuffer buffer, @NotNull GameType type) {
        final var size = buffer.getInt();
        final var hash = BufferUtils.getInts(buffer, 4);
        final var unk0 = buffer.getInt();
        final var unk1 = buffer.getInt();
        final var unk2 = buffer.getInt();
        final var programs = BufferUtils.getObjects(buffer, type == GameType.HZD ? 4 : buffer.getInt(), Entry[]::new, b -> Entry.read(b, type));
        final var rootSignature = BufferUtils.getBytes(buffer, buffer.getInt());

        return new HwShader(unk0, unk1, unk2, programs, rootSignature);
    }

    public record Entry(@NotNull int[] header, @NotNull Program program) {
        @NotNull
        public static Entry read(@NotNull ByteBuffer buffer, @NotNull GameType type) {
            final var header = BufferUtils.getInts(buffer, type == GameType.HZD ? 11 : 12);
            final var program = Program.read(buffer, buffer.getInt());

            return new Entry(header, program);
        }

        @NotNull
        public ProgramType programType() {
            return ProgramType.values()[header[5]];
        }

        public int shaderModel() {
            return header.length == 12 ? header[6] : 6;
        }

        public record Program(@NotNull byte[] blob) {
            @NotNull
            public static Program read(@NotNull ByteBuffer buffer, int size) {
                final var blob = BufferUtils.getBytes(buffer, size);
                return new Program(blob);
            }
        }

        public enum ProgramType {
            CP,
            GP,
            VP,
            FP
        }
    }
}
