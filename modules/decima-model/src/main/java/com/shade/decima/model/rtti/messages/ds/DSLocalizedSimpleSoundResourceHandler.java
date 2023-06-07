package com.shade.decima.model.rtti.messages.ds;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.RTTIClass;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTITypeRegistry;
import com.shade.decima.model.rtti.types.RTTITypeEnum;
import com.shade.decima.model.rtti.types.ds.DSDataSource;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.decima.model.rtti.types.java.RTTIField;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "LocalizedSimpleSoundResource", game = GameType.DS),
    @Type(name = "LocalizedSimpleSoundResource", game = GameType.DSDC)
})
public class DSLocalizedSimpleSoundResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final RTTITypeEnum ELanguage = registry.find("ELanguage");

        final int bits = buffer.getShort() & 0xffff;
        final List<RTTIObject> entries = new ArrayList<>(25);

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

        for (int i = 0, j = 0; i < 25; i++) {
            final RTTITypeEnum.Constant language = ELanguage.valueOf(i + 1);

            if ((getFlags(language) & 2) == 0) {
                continue;
            }

            if ((bits & (1 << j)) != 0) {
                entries.add(Entry.read(registry, buffer, language));
            }

            j += 1;
        }

        object.set("Wave", wave);
        object.set("Entries", entries.toArray(RTTIObject[]::new));
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
        return new Component[]{
            new Component("Wave", registry.find("WaveResource")),
            new Component("Entries", registry.find(Entry[].class))
        };
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
        @RTTIField(type = @Type(name = "uint8"))
        public byte unk;

        @NotNull
        public static RTTIObject read(@NotNull RTTITypeRegistry registry, @NotNull ByteBuffer buffer, @NotNull RTTITypeEnum.Constant language) {
            final var object = new Entry();
            object.unk = buffer.get();
            object.dataSource = DSDataSource.read(registry, buffer);
            object.language = language;

            return new RTTIObject(registry.find(Entry.class), object);
        }
    }
}
