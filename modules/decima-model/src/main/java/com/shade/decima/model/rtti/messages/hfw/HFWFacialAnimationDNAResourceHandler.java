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

        RigLogic rigLogic = RigLogic.get(buffer);

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
        Configuration config,
        RigMetrics metrics,
        Controls controls,
        Joints joints,
        BlendShapes blendShapes,
        AnimatedMaps animatedMaps
    ) {
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
        CalculationType calculationType
    ) {
        public static Configuration get(@NotNull ByteBuffer buffer) {
            return new Configuration(CalculationType.values()[buffer.getInt()]);
        }

        private enum CalculationType {
            Scalar,  ///< scalar CPU algorithm
            SSE,  ///< vectorized (SSE) CPU algorithm
            AVX  ///< vectorized (AVX) CPU algorithm (RigLogic must be built with AVX support,
            ///< otherwise it falls back to using the Scalar version)
        }

        ;
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
        public static RigMetrics get(@NotNull ByteBuffer buffer) {
            return new RigMetrics(buffer.getShort(), buffer.getShort(), buffer.getShort(), buffer.getShort(),
                buffer.getShort(), buffer.getShort(), buffer.getShort());
        }
    }

    private record Controls(
        ConditionalTable guiToRawMapping,
        PSDMatrix psds
    ) {
        public static Controls get(@NotNull ByteBuffer buffer) {
            return new Controls(ConditionalTable.get(buffer), PSDMatrix.get(buffer));
        }
    }

    private record PSDMatrix(
        short distinctPSDs,
        short[] rowIndices,
        short[] columnIndices,
        float[] values
    ) {
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
        Evaluator evaluator,
        float[] neutralValues,
        short[][] variableAttributeIndices,
        short jointGroupCount
    ) {
        public static Joints get(@NotNull ByteBuffer buffer) {
            final var evaluator = Evaluator.get(buffer);
            final var neutralValues = BufferUtils.getFloats(buffer, buffer.getInt());

            final var variableAttributeIndices = new short[buffer.getInt()][];
            for (int i = 0; i < variableAttributeIndices.length; i++) {
                variableAttributeIndices[i] = BufferUtils.getShorts(buffer, buffer.getInt());
            }

            final var jointGroupCount = buffer.getShort();
            return new Joints(evaluator, neutralValues, variableAttributeIndices, jointGroupCount);
        }
    }

    private record BlendShapes(
        short[] lods,
        short[] inputIndices,
        short[] outputIndices
    ) {
        public static BlendShapes get(@NotNull ByteBuffer buffer) {
            return new BlendShapes(
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getShorts(buffer, buffer.getInt()),
                BufferUtils.getShorts(buffer, buffer.getInt())
            );
        }
    }

    private record AnimatedMaps(
        short[] lods,
        ConditionalTable conditionals
    ) {
        public static AnimatedMaps get(@NotNull ByteBuffer buffer) {
            return new AnimatedMaps(BufferUtils.getShorts(buffer, buffer.getInt()), ConditionalTable.get(buffer));
        }
    }

    private record ConditionalTable(
        short[] inputIndices,
        short[] outputIndices,
        float[] fromValues,
        float[] toValues,
        float[] slopeValues,
        float[] cutValues,
        short inputCount,
        short outputCount
    ) {
        public static ConditionalTable get(@NotNull ByteBuffer buffer) {
            final var unk1 = BufferUtils.getShorts(buffer, buffer.getInt());
            final var unk2 = BufferUtils.getShorts(buffer, buffer.getInt());
            final var unk3 = BufferUtils.getFloats(buffer, buffer.getInt());
            final var unk4 = BufferUtils.getFloats(buffer, buffer.getInt());
            final var unk5 = BufferUtils.getFloats(buffer, buffer.getInt());
            final var unk6 = BufferUtils.getFloats(buffer, buffer.getInt());
            final var unk7 = buffer.getShort();
            final var unk8 = buffer.getShort();

            return new ConditionalTable(unk1, unk2, unk3, unk4, unk5, unk6, unk7, unk8);
        }
    }

    private record Evaluator(
        JointStorage storage
    ) {
        public static Evaluator get(@NotNull ByteBuffer buffer) {
            final var unk2 = JointStorage.get(buffer);

            return new Evaluator(unk2);
        }
    }

    private record JointStorage(

        short[] values,
        short[] inputIndices,
        short[] outputIndices,
        LODRegion[] lodRegions,
        JointGroup[] jointGroups
    ) {
        record LODRegion(int size, int sizeAlignedToLastFullBlock, int sizeAlignedToSecondLastFullBlock) {
            public static LODRegion get(@NotNull ByteBuffer buffer) {
                final var unk1 = buffer.getInt();
                final var unk2 = buffer.getInt();
                final var unk3 = buffer.getInt();

                return new LODRegion(unk1, unk2, unk3);
            }
        }

        record JointGroup(int valuesOffset, int inputIndicesOffset, int outputIndicesOffset, int lodsOffset,
                          int valuesSize, int inputIndicesSize, int inputIndicesSizeAlignedTo4,
                          int inputIndicesSizeAlignedTo8) {
            public static JointGroup get(@NotNull ByteBuffer buffer) {
                final var unk1 = buffer.getInt();
                final var unk2 = buffer.getInt();
                final var unk3 = buffer.getInt();
                final var unk4 = buffer.getInt();
                final var unk5 = buffer.getInt();
                final var unk6 = buffer.getInt();
                final var unk7 = buffer.getInt();
                final var unk8 = buffer.getInt();

                return new JointGroup(unk1, unk2, unk3, unk4, unk5, unk6, unk7, unk8);
            }
        }

        public static JointStorage get(@NotNull ByteBuffer buffer) {
            final var unk1 = BufferUtils.getShorts(buffer, buffer.getInt());
            final var unk2 = BufferUtils.getShorts(buffer, buffer.getInt());
            final var unk3 = BufferUtils.getShorts(buffer, buffer.getInt());
            final var unk4 = BufferUtils.getObjects(buffer, buffer.getInt(), LODRegion[]::new, LODRegion::get);
            final var unk5 = BufferUtils.getObjects(buffer, buffer.getInt(), JointGroup[]::new, JointGroup::get);

            return new JointStorage(unk1, unk2, unk3, unk4, unk5);
        }
    }

}
