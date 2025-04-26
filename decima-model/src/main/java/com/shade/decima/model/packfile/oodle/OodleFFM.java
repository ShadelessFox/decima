package com.shade.decima.model.packfile.oodle;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

final class OodleFFM {
    private final MethodHandle OodleLZ_Compress;
    private final MethodHandle OodleLZ_Decompress;
    private final MethodHandle Oodle_GetConfigValues;

    OodleFFM(Path path, Arena arena) {
        SymbolLookup lookup = SymbolLookup.libraryLookup(path, arena);
        Linker linker = Linker.nativeLinker();

        OodleLZ_Compress = linker.downcallHandle(
            lookup.findOrThrow("OodleLZ_Compress"),
            FunctionDescriptor.of(
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_INT,  // compressor
                ValueLayout.ADDRESS,   // rawBuf
                ValueLayout.JAVA_LONG, // rawLen
                ValueLayout.ADDRESS,   // compBuf
                ValueLayout.JAVA_INT,  // level
                ValueLayout.ADDRESS,   // pOptions
                ValueLayout.ADDRESS,   // dictionaryBase
                ValueLayout.ADDRESS,   // lrm
                ValueLayout.ADDRESS,   // scratchMem
                ValueLayout.JAVA_LONG  // scratchSize
            )
        );
        OodleLZ_Decompress = linker.downcallHandle(
            lookup.findOrThrow("OodleLZ_Decompress"),
            FunctionDescriptor.of(
                ValueLayout.JAVA_LONG,
                ValueLayout.ADDRESS,   // compBuf
                ValueLayout.JAVA_LONG, // compBufSize
                ValueLayout.ADDRESS,   // rawBuf
                ValueLayout.JAVA_LONG, // rawLen
                ValueLayout.JAVA_INT,  // fuzzSafe
                ValueLayout.JAVA_INT,  // checkCRC
                ValueLayout.JAVA_INT,  // verbosity
                ValueLayout.ADDRESS,   // decBufBase
                ValueLayout.JAVA_LONG, // decBufSize
                ValueLayout.ADDRESS,   // fpCallback
                ValueLayout.ADDRESS,   // callbackUserData
                ValueLayout.ADDRESS,   // decoderMemory
                ValueLayout.JAVA_LONG, // decoderMemorySize
                ValueLayout.JAVA_INT   // threadPhase
            )
        );
        Oodle_GetConfigValues = linker.downcallHandle(
            lookup.findOrThrow("Oodle_GetConfigValues"),
            FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS // ptr
            )
        );
    }

    public long OodleLZ_Compress(int compressor, MemorySegment rawBuf, long rawLen, MemorySegment compBuf, int level, MemorySegment pOptions, MemorySegment dictionaryBase, MemorySegment lrm, MemorySegment scratchMem, long scratchSize) {
        try {
            return (long) OodleLZ_Compress.invokeExact(compressor, rawBuf, rawLen, compBuf, level, pOptions, dictionaryBase, lrm, scratchMem, scratchSize);
        } catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }

    public long OodleLZ_Decompress(MemorySegment compBuf, long compBufSize, MemorySegment rawBuf, long rawLen, int fuzzSafe, int checkCRC, int verbosity, MemorySegment decBufBase, long decBufSize, MemorySegment fpCallback, MemorySegment callbackUserData, MemorySegment decoderMemory, long decoderMemorySize, int threadPhase) {
        try {
            return (long) OodleLZ_Decompress.invokeExact(compBuf, compBufSize, rawBuf, rawLen, fuzzSafe, checkCRC, verbosity, decBufBase, decBufSize, fpCallback, callbackUserData, decoderMemory, decoderMemorySize, threadPhase);
        } catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }

    public void Oodle_GetConfigValues(MemorySegment segment) {
        try {
            Oodle_GetConfigValues.invokeExact(segment);
        } catch (Throwable e) {
            throw new AssertionError("should not reach here", e);
        }
    }
}