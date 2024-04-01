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

    /// Height data
    int sampleCount = 0;                    ///< See HeightFieldShapeSettings::mSampleCount
    int blockSize = 2;                        ///< See HeightFieldShapeSettings::mBlockSize
    byte bitsPerSample = 8;                    ///< See HeightFieldShapeSettings::mBitsPerSample
    byte sampleMask = (byte) 0xff;                    ///< All bits set for a sample: (1 << mBitsPerSample) - 1, used to indicate that there's no collision
    short minSample = HeightFieldShapeConstants.cNoCollisionValue16;    ///< Min and max value in mHeightSamples quantized to 16 bit, for calculating bounding box
    short maxSample = HeightFieldShapeConstants.cNoCollisionValue16;
    RangeBlock[] rangeBlocks;                        ///< Hierarchical grid of range data describing the height variations within 1 block. The grid for level <level> starts at offset sGridOffsets[<level>]
    byte[] heightSamples;                        ///< mBitsPerSample-bit height samples. Value [0, mMaxHeightValue] maps to highest detail grid in mRangeBlocks [mMin, mMax]. mNoCollisionValue is reserved to indicate no collision.
    byte[] activeEdges;                        ///< (mSampleCount - 1)^2 * 3-bit active edge flags.

    /// Materials
    PhysicsMaterial[] materials;                            ///< The materials of square at (x, y) is: mMaterials[mMaterialIndices[x + y * (mSampleCount - 1)]]
    byte[] materialIndices;                    ///< Compressed to the minimum amount of bits per material index (mSampleCount - 1) * (mSampleCount - 1) * mNumBitsPerMaterialIndex bits of data
    int numBitsPerMaterialIndex = 0;        ///< Number of bits per material index

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
