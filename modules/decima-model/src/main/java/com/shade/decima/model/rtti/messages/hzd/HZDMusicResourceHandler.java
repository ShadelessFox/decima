package com.shade.decima.model.rtti.messages.hzd;

import com.shade.decima.model.base.GameType;
import com.shade.decima.model.rtti.Type;
import com.shade.decima.model.rtti.messages.MessageHandler;
import com.shade.decima.model.rtti.messages.MessageHandlerRegistration;
import com.shade.decima.model.rtti.objects.RTTIObject;
import com.shade.decima.model.rtti.registry.RTTIFactory;
import com.shade.decima.model.rtti.types.hzd.HZDDataSource;
import com.shade.decima.model.rtti.types.java.HwDataSource;
import com.shade.platform.model.util.BufferUtils;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

@MessageHandlerRegistration(message = "MsgReadBinary", types = {
    @Type(name = "MusicResource", game = GameType.HZD),
})
public class HZDMusicResourceHandler implements MessageHandler.ReadBinary {
    @Override
    public void read(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final byte[] musicData = BufferUtils.getBytes(buffer, buffer.getInt());
        final RTTIObject[] dataSources = new RTTIObject[object.<String[]>get("StreamingBankNames").length];

        for (int i = 0; i < dataSources.length; i++) {
            dataSources[i] = HZDDataSource.read(factory, buffer);
        }

        object.set("MusicData", musicData);
        object.set("DataSources", dataSources);
    }

    @Override
    public void write(@NotNull RTTIFactory factory, @NotNull ByteBuffer buffer, @NotNull RTTIObject object) {
        final byte[] musicData = object.get("MusicData");
        final RTTIObject[] dataSources = object.get("DataSources");

        buffer.putInt(musicData.length);
        buffer.put(musicData);

        for (RTTIObject dataSource : dataSources) {
            dataSource.<HwDataSource>cast().write(factory, buffer);
        }
    }

    @Override
    public int getSize(@NotNull RTTIFactory factory, @NotNull RTTIObject object) {
        final byte[] musicData = object.get("MusicData");
        final RTTIObject[] dataSources = object.get("DataSources");

        return 4 + musicData.length + Arrays.stream(dataSources)
            .map(RTTIObject::<HwDataSource>cast)
            .mapToInt(HwDataSource::getSize)
            .sum();
    }

    @NotNull
    @Override
    public Component[] components(@NotNull RTTIFactory factory) {
        return new Component[]{
            new Component("MusicData", factory.find("Array<uint8>")),
            new Component("DataSources", factory.find(HwDataSource[].class)),
        };
    }
}
