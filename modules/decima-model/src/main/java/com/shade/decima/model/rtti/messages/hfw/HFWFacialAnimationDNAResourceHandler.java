package com.shade.decima.model.rtti.messages.hfw;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "FacialAnimationDNAResource", game = GameType.HFW),
})
public class HFWFacialAnimationDNAResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        buffer.order(ByteOrder.BIG_ENDIAN);

        // TODO: Not used now
        final var rigLogic = RigLogic.get(buffer);

        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        throw new NotImplementedException();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[0];
    }

    private record RigLogic(
        @NotNull Configuration config,
        @NotNull RigMetrics metrics,
        @NotNull Controls controls,
        @NotNull Joints joints,
        @NotNull BlendShapes blendShapes,
        @NotNull AnimatedMaps animatedMaps
    ) {
        @NotNull
        public static RigLogic get(@NotNull ByteBuffer buffer) {
            return new RigLogic(
                Configuration.get(buffer),
                RigMetrics.get(buffer),
                Controls.get(buffer),
                Joints.get(buffer),
                BlendShapes.get(buffer),
                AnimatedMaps.get(buffer)
            );
        }
    }

    private record Configuration(
        @NotNull CalculationType calculationType
    ) {
        @NotNull
        public static Configuration get(@NotNull ByteBuffer buffer) {
            return new Configuration(
                CalculationType.values()[buffer.getInt()]
            );
        }

        private enum CalculationType {
            Scalar,
            SSE,
            AVX
        }
    }

    private record RigMetrics(
        short lodCount,
        short guiControlCount,
        short rawControlCount,
        short psdCount,
        short jointAttributeCount,
        short blendShapeCount,
        short animatedMapCount
    ) {
        @NotNull
        public static RigMetrics get(@NotNull ByteBuffer buffer) {
            return new RigMetrics(
                buffer.getShort(),
                buffer.getShort(),
                buffer.getShort(),
                buffer.getShort(),
                buffer.getShort(),
                buffer.getShort(),
                buffer.getShort()
            );
        }
    }

    private record Controls(
        @NotNull ConditionalTable guiToRawMapping,
        @NotNull PSDMatrix psds
    ) {
        @NotNull
        public static Controls get(@NotNull ByteBuffer buffer) {
            return new Controls(
                ConditionalTable.get(buffer),
                PSDMatrix.get(buffer)
            );
        }
    }

    private record PSDMatrix(
        short distinctPSDs,
        @NotNull short[] rowIndices,
        @NotNull short[] columnIndices,
        @NotNull float[] values
    ) {
        @NotNull
        public static PSDMatrix get(@NotNull ByteBuffer buffer) {
            return new PSDMatrix(
                buffer.getShort(),
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getFloats(buffer, buffer.getInt())
            );
        }
    }

    private record Joints(
        @NotNull Evaluator evaluator,
        @NotNull float[] neutralValues,
        @NotNull short[][] variableAttributeIndices,
        short jointGroupCount
    ) {
        public static Joints get(@NotNull ByteBuffer buffer) {
            return new Joints(
                Evaluator.get(buffer),
                BufferUtils.getFloats(buffer, buffer.getInt()),
                BufferUtils.getObjects(buffer, buffer.getInt(), short[][]::new, buf -> BufferUtils.getShorts(buf, buf.getInt())),
                buffer.getShort()
            );
        }
    }

    private record BlendShapes(
        @NotNull short[] lods,
        @NotNull short[] inputIndices,
        @NotNull short[] outputIndices
    ) {
        @NotNull
        public static BlendShapes get(@NotNull ByteBuffer buffer) {
            return new BlendShapes(
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getShorts(buffer, buffer.getInt())
            );
        }
    }

    private record AnimatedMaps(
        @NotNull short[] lods,
        @NotNull ConditionalTable conditionals
    ) {
        @NotNull
        public static AnimatedMaps get(@NotNull ByteBuffer buffer) {
            return new AnimatedMaps(
                BufferUtils.getShorts(buffer, buffer.getInt()),
                ConditionalTable.get(buffer)
            );
        }
    }

    private record ConditionalTable(
        @NotNull short[] inputIndices,
        @NotNull short[] outputIndices,
        @NotNull float[] fromValues,
        @NotNull float[] toValues,
        @NotNull float[] slopeValues,
        @NotNull float[] cutValues,
        short inputCount,
        short outputCount
    ) {
        @NotNull
        public static ConditionalTable get(@NotNull ByteBuffer buffer) {
            return new ConditionalTable(
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getFloats(buffer, buffer.getInt()),
                BufferUtils.getFloats(buffer, buffer.getInt()),
                BufferUtils.getFloats(buffer, buffer.getInt()),
                BufferUtils.getFloats(buffer, buffer.getInt()),
                buffer.getShort(),
                buffer.getShort()
            );
        }
    }

    private record Evaluator(
        @NotNull JointStorage storage
    ) {
        @NotNull
        public static Evaluator get(@NotNull ByteBuffer buffer) {
            return new Evaluator(JointStorage.get(buffer));
        }
    }

    private record JointStorage(
        @NotNull short[] values,
        @NotNull short[] inputIndices,
        @NotNull short[] outputIndices,
        @NotNull LODRegion[] lodRegions,
        @NotNull JointGroup[] jointGroups
    ) {
        record LODRegion(int size, int sizeAlignedToLastFullBlock, int sizeAlignedToSecondLastFullBlock) {
            @NotNull
            public static LODRegion get(@NotNull ByteBuffer buffer) {
                return new LODRegion(
                    buffer.getInt(),
                    buffer.getInt(),
                    buffer.getInt()
                );
            }
        }

        record JointGroup(
            int valuesOffset,
            int inputIndicesOffset,
            int outputIndicesOffset,
            int lodsOffset,
            int valuesSize,
            int inputIndicesSize,
            int inputIndicesSizeAlignedTo4,
            int inputIndicesSizeAlignedTo8
        ) {
            @NotNull
            public static JointGroup get(@NotNull ByteBuffer buffer) {
                return new JointGroup(
                    buffer.getInt(),
                    buffer.getInt(),
                    buffer.getInt(),
                    buffer.getInt(),
                    buffer.getInt(),
                    buffer.getInt(),
                    buffer.getInt(),
                    buffer.getInt()
                );
            }
        }

        @NotNull
        public static JointStorage get(@NotNull ByteBuffer buffer) {
            return new JointStorage(
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getObjects(buffer, buffer.getInt(), LODRegion[]::new, LODRegion::get),
                BufferUtils.getObjects(buffer, buffer.getInt(), JointGroup[]::new, JointGroup::get)
            );
        }
    }

}
