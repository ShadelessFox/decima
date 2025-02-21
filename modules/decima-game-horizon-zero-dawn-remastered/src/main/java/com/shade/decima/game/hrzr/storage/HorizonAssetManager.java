package com.shade.decima.game.hrzr.storage;

import com.shade.decima.game.AssetId;
import com.shade.decima.game.AssetManager;
import com.shade.decima.game.hrzr.rtti.HRZRTypeReader;
import com.shade.decima.game.hrzr.rtti.HorizonZeroDawnRemastered.GGUUID;
import com.shade.decima.game.hrzr.rtti.HorizonZeroDawnRemastered.RTTIRefObject;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.Closeable;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

public class HorizonAssetManager implements AssetManager, Closeable {
    private final PackFileManager packFileManager;
    private final TypeFactory typeFactory;

    public HorizonAssetManager(@NotNull PackFileManager packFileManager, @NotNull TypeFactory typeFactory) {
        this.packFileManager = packFileManager;
        this.typeFactory = typeFactory;
    }

    @NotNull
    @Override
    public <T> T get(@NotNull AssetId id, @NotNull Class<T> type) throws IOException {
        var assetId = (HorizonAssetId) id;
        var objectUUID = convertUUID(assetId.objectUuid());

        var buffer = packFileManager.load(PackFileAssetId.ofHash(assetId.fileHash()));
        var objects = new HRZRTypeReader().read(BinaryReader.wrap(buffer), typeFactory);

        var object = objects.stream()
            .map(RTTIRefObject.class::cast)
            .filter(o -> o.general().objectUUID().equals(objectUUID))
            .findFirst().orElseThrow(() -> new NoSuchElementException("Can't find asset " + id));

        return type.cast(object);
    }

    @Override
    public void close() throws IOException {
        packFileManager.close();
    }

    @NotNull
    private GGUUID convertUUID(@NotNull UUID uuid) {
        var msb = uuid.getMostSignificantBits();
        var lsb = uuid.getLeastSignificantBits();

        var object = typeFactory.newInstance(GGUUID.class);
        object.data0((byte) (msb >>> 56));
        object.data1((byte) (msb >>> 48));
        object.data2((byte) (msb >>> 40));
        object.data3((byte) (msb >>> 32));
        object.data4((byte) (msb >>> 24));
        object.data5((byte) (msb >>> 16));
        object.data6((byte) (msb >>> 8));
        object.data7((byte) (msb));
        object.data8((byte) (lsb >>> 56));
        object.data9((byte) (lsb >>> 48));
        object.data10((byte) (lsb >>> 40));
        object.data11((byte) (lsb >>> 32));
        object.data12((byte) (lsb >>> 24));
        object.data13((byte) (lsb >>> 16));
        object.data14((byte) (lsb >>> 8));
        object.data15((byte) (lsb));

        return object;
    }
}
