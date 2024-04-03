package com.shade.decima.hfw.data.jolt.physics.collision.shape;

import com.shade.decima.hfw.data.jolt.JoltUtils;
import com.shade.decima.hfw.data.jolt.physics.collision.PhysicsMaterial;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

public class HeightFieldShape extends Shape {
    public record RangeBlock(@NotNull short[] min, @NotNull short[] max) {
        @NotNull
        private static RangeBlock get(@NotNull ByteBuffer buffer) {
            return new RangeBlock(
                BufferUtils.getShorts(buffer, 4),
                BufferUtils.getShorts(buffer, 4)
            );
        }
    }

    private Vector3f offset;
    private Vector3f scale;
    private int sampleCount;
    private int blockSize;
    private byte bitsPerSample;
    private short minSample;
    private short maxSample;
    private RangeBlock[] rangeBlocks;
    private byte[] heightSamples;
    private byte[] activeEdges;
    private byte[] materialIndices;
    private int numBitsPerMaterialIndex;
    private PhysicsMaterial[] materials;

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
        rangeBlocks = JoltUtils.getArray(buffer, RangeBlock[]::new, RangeBlock::get);
        heightSamples = JoltUtils.getByteArray(buffer);
        activeEdges = JoltUtils.getByteArray(buffer);
        materialIndices = JoltUtils.getByteArray(buffer);
        numBitsPerMaterialIndex = buffer.getInt();
    }

    @Override
    public void restoreMaterialState(@NotNull PhysicsMaterial[] materials) {
        this.materials = materials;
    }
}
