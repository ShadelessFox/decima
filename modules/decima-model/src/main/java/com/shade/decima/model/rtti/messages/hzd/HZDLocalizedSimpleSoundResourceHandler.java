package com.shade.decima.model.rtti.messages.hzd;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.RTTIEnum;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.hzd.HZDDataSource;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "LocalizedSimpleSoundResource", game = GameType.HZD)
})
public class HZDLocalizedSimpleSoundResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final String location = BufferUtils.getString(buffer, buffer.getInt());
        final int mask = buffer.getShort() & 0xffff;
        final byte size = buffer.get();
        final byte flags = buffer.get();
        final List<RTTIObject> entries = new ArrayList<>();

        if (size != 28) {
            throw new IllegalStateException("Entry size mismatch: " + size + " != 28");
        }

        final RTTIObject wave = registry.<RTTIClass>find("WaveResource").instantiate();
        wave.set("IsStreaming", (flags & 1) != 0);
        wave.set("UseVBR", (flags & 2) != 0);
        wave.set("EncodingQuality", registry.<RTTITypeEnum>find("EWaveDataEncodingQuality").valueOf((flags >> 2 & 15)));
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
                entries.add(Entry.read(registry, buffer, language, location));
            }
            shift += 1;
        }

        object.set("Location", location);
        object.set("WaveData", wave);
        object.set("DataSources", entries.toArray(RTTIObject[]::new));
    }

    @Override
    public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final var location = object.str("Location").getBytes(StandardCharsets.UTF_8);
        final var wave = object.obj("WaveData");
        final var dataSources = object.objs("DataSources");

        buffer.putInt(location.length);
        buffer.put(location);
        buffer.putShort((short) computeMask(registry, dataSources));
        buffer.put((byte) 28);
        buffer.put((byte) computeFlags(wave));
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
        return 26
            + object.str("Location").getBytes(StandardCharsets.UTF_8).length
            + object.objs("DataSources").length * Entry.getSize();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTITypeRegistry registry) {
        return new Component[]{
            new Component("Location", registry.find("String")),
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

    private static int computeFlags(@NotNull RTTIObject wave) {
        int flags = 0;
        flags |= wave.bool("IsStreaming") ? 1 : 0;
        flags |= wave.bool("UseVBR") ? 2 : 0;
        flags |= wave.<RTTITypeEnum.Constant>get("EncodingQuality").value() << 2;
        return flags;
    }

    private static int computeMask(@NotNull RTTITypeRegistry registry, @NotNull RTTIObject[] dataSources) {
        int mask = 0;

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
        return mask;
    }

    private static int getFlags(@NotNull RTTITypeEnum.Constant language) {
        // See sub_7FF6BFD65BD0
        return switch (language.value()) {
            case 1 -> 7;
            case 2, 3, 4, 5, 7, 10, 11, 16, 17, 18, 20 -> 3;
            default -> 1;
        };
    }

    public static class Entry {
        @RTTIField(type = @Type(name = "ELanguage"))
        public RTTITypeEnum.Constant language;
        @RTTIField(type = @Type(type = HwDataSource.class))
        public RTTIObject dataSource;
        @RTTIField(type = @Type(name = "uint64"))
        public long sampleCount;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIEnum.Constant language, @NotNull String location) {
            final var waveDataSize = buffer.getInt();
            final var sampleCount = buffer.getLong();
            final var offset = buffer.getLong();
            final var length = buffer.getLong();
            assert waveDataSize == length;

            final var dataSource = new HZDDataSource();
            dataSource.location = "%s.%s.stream".formatted(location, language.name().toLowerCase(Locale.ROOT));
            dataSource.offset = offset;
            dataSource.length = length;

            final var object = new Entry();
            object.language = language;
            object.dataSource = new RTTIObject(registry.find(HZDDataSource.class), dataSource);
            object.sampleCount = sampleCount;

            return new RTTIObject(registry.find(Entry.class), object);
        }

        public void write(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer) {
            final HZDDataSource dataSource = this.dataSource.cast();

            buffer.putInt((int) dataSource.length);
            buffer.putLong(sampleCount);
            buffer.putLong(dataSource.offset);
            buffer.putLong(dataSource.length);
        }

        public static int getSize() {
            return 28;
        }
    }
}
