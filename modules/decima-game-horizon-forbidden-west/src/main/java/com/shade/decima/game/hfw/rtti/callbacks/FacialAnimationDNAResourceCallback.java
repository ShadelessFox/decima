package com.shade.decima.game.hfw.rtti.callbacks;

import com.shade.decima.game.hfw.data.riglogic.RigLogic;
import com.shade.decima.rtti.data.ExtraBinaryDataCallback;
import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.util.NotNull;
import com.shade.util.io.BinaryReader;

import java.io.IOException;
import java.nio.ByteOrder;

public class FacialAnimationDNAResourceCallback implements ExtraBinaryDataCallback<FacialAnimationDNAResourceCallback.FacialAnimationDNAData> {
    public interface FacialAnimationDNAData {
    }

    @Override
    public void deserialize(@NotNull BinaryReader reader, @NotNull TypeFactory factory, @NotNull FacialAnimationDNAData object) throws IOException {
        reader.order(ByteOrder.BIG_ENDIAN);
        var rigLogic = RigLogic.read(reader); // FIXME: Not used now
        reader.order(ByteOrder.LITTLE_ENDIAN);
    }
}
