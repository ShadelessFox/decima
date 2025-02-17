import com.shade.decima.game.hfw.rtti.callbacks.*;
import com.shade.decima.game.hfw.rtti.data.StreamingDataSourceExtension;
import com.shade.decima.rtti.generator.GenerateBindings;
import com.shade.decima.rtti.generator.GenerateBindings.Builtin;
import com.shade.decima.rtti.generator.GenerateBindings.Callback;
import com.shade.decima.rtti.generator.GenerateBindings.Extension;

import java.math.BigInteger;

@GenerateBindings(
    source = "data/horizon_forbidden_west_rtti.json",
    target = "com.shade.decima.game.hfw.rtti.HorizonForbiddenWest",
    builtins = {
        @Builtin(type = "bool", javaType = boolean.class),
        @Builtin(type = "int", javaType = int.class),
        @Builtin(type = "int8", javaType = byte.class),
        @Builtin(type = "int16", javaType = short.class),
        @Builtin(type = "int32", javaType = int.class),
        @Builtin(type = "int64", javaType = long.class),
        @Builtin(type = "intptr", javaType = long.class),
        @Builtin(type = "uint", javaType = int.class),
        @Builtin(type = "uint8", javaType = byte.class),
        @Builtin(type = "uint16", javaType = short.class),
        @Builtin(type = "uint32", javaType = int.class),
        @Builtin(type = "uint64", javaType = long.class),
        @Builtin(type = "uint128", javaType = BigInteger.class),
        @Builtin(type = "uintptr", javaType = long.class),
        @Builtin(type = "float", javaType = float.class),
        @Builtin(type = "double", javaType = double.class),
        @Builtin(type = "HalfFloat", javaType = float.class),
        @Builtin(type = "tchar", javaType = char.class),
        @Builtin(type = "wchar", javaType = char.class),
        @Builtin(type = "ucs4", javaType = int.class),
        @Builtin(type = "String", javaType = String.class),
        @Builtin(type = "WString", javaType = String.class),
    },
    callbacks = {
        @Callback(type = "DataBufferResource", handler = DataBufferResourceCallback.class),
        @Callback(type = "DebugMouseCursorPS4", handler = DebugMouseCursorPS4Callback.class),
        @Callback(type = "FacialAnimationDNAResource", handler = FacialAnimationDNAResourceCallback.class),
        @Callback(type = "IndexArrayResource", handler = IndexArrayResourceCallback.class),
        @Callback(type = "LocalizedTextResource", handler = LocalizedTextResourceCallback.class),
        @Callback(type = "MorphemeAnimation", handler = MorphemeAnimationCallback.class),
        @Callback(type = "MorphemeAsset", handler = MorphemeAssetCallback.class),
        @Callback(type = "MorphemeNetworkDefResource", handler = MorphemeNetworkDefResourceCallback.class),
        @Callback(type = "MorphemeNetworkInstancePreInitializedData", handler = MorphemeNetworkInstancePreInitializedDataCallback.class),
        @Callback(type = "PhysicsRagdollResource", handler = PhysicsRagdollResourceCallback.class),
        @Callback(type = "PhysicsShapeResource", handler = PhysicsShapeResourceCallback.class),
        @Callback(type = "Pose", handler = PoseCallback.class),
        @Callback(type = "ShaderResource", handler = ShaderResourceCallback.class),
        @Callback(type = "StaticTile", handler = StaticTileCallback.class),
        @Callback(type = "Texture", handler = TextureCallback.class),
        @Callback(type = "TextureList", handler = TextureListCallback.class),
        @Callback(type = "UITexture", handler = UITextureCallback.class),
        @Callback(type = "UITextureFrames", handler = UITextureFramesCallback.class),
        @Callback(type = "VertexArrayResource", handler = VertexArrayResourceCallback.class),
        @Callback(type = "WorldMapSuperTile", handler = WorldMapSuperTileCallback.class),
    },
    extensions = {
        @Extension(type = "StreamingDataSource", extension = StreamingDataSourceExtension.class),
    }
)
module decima.game.hfw {
    requires static decima.rtti.generator;

    requires decima.rtti;
    requires decima.game;

    requires org.slf4j;

    opens com.shade.decima.game.hfw.rtti to decima.rtti;
    opens com.shade.decima.game.hfw.rtti.callbacks to decima.rtti;

    exports com.shade.decima.game.hfw.storage;
    exports com.shade.decima.game.hfw.rtti;
}
