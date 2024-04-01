package com.shade.decima.model.rtti.types.jolt.physics.collision.shape;

import com.shade.decima.model.rtti.types.jolt.JoltUtils;
import com.shade.decima.model.rtti.types.jolt.physics.collision.PhysicsMaterial;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class HeightFieldShape extends Shape {
    Vector3f offset = new Vector3f(0.0f);
    Vector3f scale = new Vector3f(1.0f);
    int sampleCount = 0;
    int blockSize = 2;
    byte bitsPerSample = 8;
    byte sampleMask = (byte) 0xff;
    short minSample = HeightFieldShapeConstants.cNoCollisionValue16;
    short maxSample = HeightFieldShapeConstants.cNoCollisionValue16;
    RangeBlock[] rangeBlocks;
    byte[] heightSamples;
    byte[] activeEdges;
    PhysicsMaterial[] materials;
    byte[] materialIndices;
    int numBitsPerMaterialIndex = 0;

    @Override
    public void restoreBinaryState(@NotNull ByteBuffer buffer) {
        super.restoreBinaryState(buffer);
        offset = JoltUtils.getAlignedVector3(buffer);
        scale = JoltUtils.getAlignedVector3(buffer);
        sampleCount = buffer.getInt();
        blockSize = buffer.getInt();
        bitsPerSample = buffer.get();
        minSample = buffer.getShort();
        maxSample = buffer.getShort();
        rangeBlocks = BufferUtils.getObjects(buffer, Math.toIntExact(buffer.getLong()), RangeBlock[]::new, RangeBlock::get);
        heightSamples = BufferUtils.getBytes(buffer, Math.toIntExact(buffer.getLong()));
        activeEdges = BufferUtils.getBytes(buffer, Math.toIntExact(buffer.getLong()));
        materialIndices = BufferUtils.getBytes(buffer, Math.toIntExact(buffer.getLong()));
        numBitsPerMaterialIndex = buffer.getInt();

        sampleMask = (byte) ((1 << bitsPerSample) - 1);

    }

    @Override
    public void restoreMaterialState(@NotNull PhysicsMaterial[] materials) {
        this.materials = materials;
    }

    static class HeightFieldShapeConstants {
        /// Value used to create gaps in the height field
        static float cNoCollisionValue = Float.MAX_VALUE;

        /// Stack size to use during WalkHeightField
        static int cStackSize = 128;

        /// A position in the hierarchical grid is defined by a level (which grid), x and y position. We encode this in a single uint32 as: level << 28 | y << 14 | x
        static int cNumBitsXY = 14;
        static int cMaskBitsXY = (1 << cNumBitsXY) - 1;
        static int cLevelShift = 2 * cNumBitsXY;

        /// When height samples are converted to 16 bit:
        static short cNoCollisionValue16 = (short) 0xffff;        ///< This is the magic value for 'no collision'
        static short cMaxHeightValue16 = (short) 0xfffe;            ///< This is the maximum allowed height value
    }

    private record RangeBlock(
        short[] min,
        short[] max
    ) {
        @NotNull
        public static RangeBlock get(@NotNull ByteBuffer buffer) {
            return new RangeBlock(BufferUtils.getShorts(buffer, 4), BufferUtils.getShorts(buffer, 4));
        }
    }
}
