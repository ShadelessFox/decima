package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIBinaryReader;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
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
    public void read(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer) {
        final int mask = buffer.getShort() & 0xffff;
        final List<RTTIObject> dataSources = new ArrayList<>();

        final RTTIObject wave = factory.<RTTIClass>find("WaveResource").create();
        wave.set("IsStreaming", buffer.get() != 0);
        wave.set("UseVBR", buffer.get() != 0);
        wave.set("EncodingQuality", factory.<RTTITypeEnum>find("EWaveDataEncodingQuality").valueOf(buffer.get() & 0xff));
        wave.set("FrameSize", buffer.getShort());
        wave.set("Encoding", factory.<RTTITypeEnum>find("EWaveDataEncoding").valueOf(buffer.get() & 0xff));
        wave.set("ChannelCount", buffer.get());
        wave.set("SampleRate", buffer.getInt());
        wave.set("BitsPerSample", buffer.getShort());
        wave.set("BitsPerSecond", buffer.getInt());
        wave.set("BlockAlignment", buffer.getShort());
        wave.set("FormatTag", buffer.getShort());

        final List<RTTIEnum.Constant> languages = getSupportedLanguages(factory);
        for (int i = 0; i < languages.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                dataSources.add(Entry.read(factory, reader, buffer, languages.get(i)));
            }
        }

        object.set("WaveData", wave);
        object.set("DataSources", dataSources.toArray(RTTIObject[]::new));
    }

    @Override
    public void write(@NotNull RTTIObject object, @NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
        final RTTIObject[] dataSources = object.objs("DataSources");
        final RTTIObject wave = object.obj("WaveData");

        buffer.putShort((short) computeMask(factory, dataSources));
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
            dataSource.<Entry>cast().write(factory, buffer);
        }
    }

    private static int computeMask(@NotNull RTTIFactory factory, RTTIObject[] dataSources) {
        int mask = 0;

        final List<RTTIEnum.Constant> supportedLanguages = getSupportedLanguages(factory);
        final List<RTTIEnum.Constant> usedLanguages = Arrays.stream(dataSources)
            .map(RTTIObject::<Entry>cast)
            .map(entry -> entry.language)
            .toList();

        for (int i = 0; i < supportedLanguages.size(); i++) {
            if (usedLanguages.contains(supportedLanguages.get(i))) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    @Override
    public int getSize(@NotNull RTTIObject object, @NotNull RTTIFactory factory) {
        return 23 + Arrays.stream(object.objs("DataSources"))
            .map(RTTIObject::<Entry>cast)
            .mapToInt(Entry::getSize)
            .sum();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("WaveData", factory.find("WaveResource")),
            new Component("DataSources", factory.find(Entry[].class))
        };
    }

    @NotNull
    private static List<RTTIEnum.Constant> getSupportedLanguages(@NotNull RTTIFactory factory) {
        return Arrays.stream(factory.<RTTIEnum>find("ELanguage").values())
            .filter(language -> (getLanguageFlags(language) & 2) != 0)
            .toList();
    }

    private static int getLanguageFlags(@NotNull RTTITypeEnum.Constant language) {
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
        public static RTTIObject read(@NotNull RTTIFactory factory, @NotNull RTTIBinaryReader reader, @NotNull ByteBuffer buffer, @NotNull RTTITypeEnum.Constant language) {
            final int length = buffer.get() & 0xff;
            assert buffer.remaining() >= length;

            final var object = new Entry();
            object.dataSource = DSDataSource.read(factory, reader, buffer);
            object.language = language;

            return new RTTIObject(factory.find(Entry.class), object);
        }

        public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer) {
            buffer.put((byte) dataSource.<DSDataSource>cast().getSize());
            dataSource.<DSDataSource>cast().write(factory, buffer);
        }

        public int getSize() {
            return dataSource.<HwDataSource>cast().getSize() + 1;
        }
    }
}
