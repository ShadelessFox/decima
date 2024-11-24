package com.shade.decima.hrzr.rtti.callbacks;

import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.shade.decima.rtti.HorizonZeroDawnRemastered.*;

public class LocalizedSimpleSoundResourceCallback implements ExtraBinaryDataCallback<LocalizedSimpleSoundResourceCallback.LocalizedSimpleSoundData> {
    public interface LocalizedSimpleSoundData {

    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull LocalizedSimpleSoundData object) throws IOException {
        var mask = Short.toUnsignedInt(reader.readShort());
        var lump = reader.readByte();

        var resource = factory.newInstance(WaveResource.class);
        resource.format().isStreaming((lump & 1) != 0);
        resource.format().useVBR((lump & 2) != 0);
        resource.format().encodingQuality(EWaveDataEncodingQuality.valueOf(lump >> 2 & 0xf));
        resource.format().frameSize(reader.readShort());
        resource.format().encoding(EWaveDataEncoding.valueOf(reader.readByte()));
        resource.format().channelCount(reader.readByte());
        resource.format().sampleRate(reader.readInt());
        resource.format().bitsPerSample(reader.readShort());
        resource.format().bitsPerSecond(reader.readInt());
        resource.format().blockAlignment(reader.readShort());
        resource.format().formatTag(reader.readShort());

        var languages = getSupportedLanguages();
        for (int i = 0; i < languages.size(); i++) {
            if ((mask & (1 << i)) != 0) {
                var length = Byte.toUnsignedInt(reader.readByte());
                var start = reader.position();

                var dataSource18 = reader.readInt();
                var dataSource1C = reader.readInt();

                var dataSource = factory.newInstance(StreamingDataSource.class);
                dataSource.channel(reader.readByte());
                dataSource.offset(reader.readInt());
                dataSource.length(reader.readInt());

                var preloadedData = reader.readBytes(reader.readInt());
            }
        }
    }

    @NotNull
    private static List<ELanguage> getSupportedLanguages() {
        return Arrays.stream(ELanguage.values())
            .filter(l -> (getLanguageFlags(l) & 2) != 0)
            .toList();
    }

    private static int getLanguageFlags(@NotNull ELanguage language) {
        return switch (language) {
            case English, French -> 7;
            case Spanish, German, Italian, Portuguese, Russian, Polish, Japanese, LATAMSP, LATAMPOR, Arabic -> 3;
            default -> 1;
        };
    }
}
