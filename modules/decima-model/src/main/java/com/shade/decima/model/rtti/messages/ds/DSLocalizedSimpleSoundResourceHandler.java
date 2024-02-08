package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.ds.DSDataSource;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "LocalizedSimpleSoundResource", game = GameType.DS),
    @Type(name = "LocalizedSimpleSoundResource", game = GameType.DSDC)
})
public class DSLocalizedSimpleSoundResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final int mask = buffer.getShort() & 0xffff;
        final List<RTTIObject> dataSources = new ArrayList<>();

        final RTTIObject wave = registry.<RTTIClass>find("WaveResource").instantiate();
        wave.set("IsStreaming", buffer.get() != 0);
        wave.set("UseVBR", buffer.get() != 0);
        wave.set("EncodingQuality", registry.<RTTITypeEnum>find("EWaveDataEncodingQuality").valueOf(buffer.get() & 0xff));
        wave.set("FrameSize", buffer.getShort());
        wave.set("Encoding", registry.<RTTITypeEnum>find("EWaveDataEncoding").valueOf(buffer.get() & 0xff));
        wave.set("ChannelCount", buffer.get());
        wave.set("SampleRate", buffer.getInt());
        wave.set("BitsPerSample", buffer.getShort());
        wave.set("BitsPerSecond", buffer.getInt());
        wave.set("BlockAlignment", buffer.getShort());
        wave.set("FormatTag", buffer.getShort());

        int shift = 0;
        for (RTTIEnum.Constant language : getLanguages(registry)) {
            if ((mask & (1 << shift)) != 0) {
                dataSources.add(Entry.read(registry, buffer, language));
            }
            shift += 1;
        }

        object.set("WaveData", wave);
        object.set("DataSources", dataSources.toArray(RTTIObject[]::new));
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        int mask = 0;

        final RTTIObject[] dataSources = object.objs("DataSources");
        final RTTIObject wave = object.obj("WaveData");

        final List<RTTIEnum.Constant> supportedLanguages = getLanguages(registry);
        final List<RTTIEnum.Constant> usedLanguages = Arrays.stream(dataSources)
            .map(RTTIObject::<Entry>cast)
            .map(entry -> entry.language)
            .toList();

        for (int i = 0; i < supportedLanguages.size(); i++) {
            if (usedLanguages.contains(supportedLanguages.get(i))) {
                mask |= 1 << i;
            }
        }

        buffer.putShort((short) mask);

        buffer.put(wave.bool("IsStreaming") ? (byte) 1 : 0);
        buffer.put(wave.bool("UseVBR") ? (byte) 1 : 0);
        buffer.put((byte) wave.<RTTITypeEnum.Constant>get("EncodingQuality").value());
        buffer.putShort(wave.i16("FrameSize"));
        buffer.put((byte) wave.<RTTITypeEnum.Constant>get("Encoding").value());
        buffer.put(wave.i8("ChannelCount"));
        buffer.putInt(wave.i32("SampleRate"));
        buffer.putShort(wave.i16("BitsPerSample"));
        buffer.putInt(wave.i32("BitsPerSecond"));
        buffer.putShort(wave.i16("BlockAlignment"));
        buffer.putShort(wave.i16("FormatTag"));

        for (RTTIObject dataSource : dataSources) {
            dataSource.<Entry>cast().write(registry, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject object) {
        return 23 + Arrays.stream(object.objs("DataSources"))
            .map(RTTIObject::<Entry>cast)
            .mapToInt(Entry::getSize)
            .sum();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("WaveData", registry.find("WaveResource")),
            new Component("DataSources", registry.find(Entry[].class))
        };
    }

    @NotNull
    private static List<RTTIEnum.Constant> getLanguages(@NotNull RTTITypeRegistry registry) {
        return Arrays.stream(registry.<RTTIEnum>find("ELanguage").values())
            .filter(language -> (getFlags(language) & 2) != 0)
            .toList();
    }

    private static int getFlags(@NotNull RTTITypeEnum.Constant language) {
        return switch (language.value()) {
            // English
            case 1 -> 7;
            // French, Spanish, German, Italian, Portuguese, Russian, Polish, Japanese, LATAMSP, LATAMPOR, Greek
            case 2, 3, 4, 5, 7, 10, 11, 16, 17, 18, 23 -> 3;
            // All others
            default -> 1;
        };
    }

    public static class Entry {
        @RTTIField(type = @Type(type = HwDataSource.class))
        public RTTIObject dataSource;
        @RTTIField(type = @Type(name = "ELanguage"))
        public RTTITypeEnum.Constant language;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTITypeEnum.Constant language) {
            final int length = buffer.get() & 0xff;
            assert buffer.remaining() >= length;

            final var object = new Entry();
            object.dataSource = DSDataSource.read(registry, buffer);
            object.language = language;

            return new RTTIObject(registry.find(Entry.class), object);
        }

        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            buffer.put((byte) dataSource.<DSDataSource>cast().getSize());
            dataSource.<DSDataSource>cast().write(registry, buffer);
        }

        public int getSize() {
            return dataSource.<HwDataSource>cast().getSize() + 1;
        }
    }
}
