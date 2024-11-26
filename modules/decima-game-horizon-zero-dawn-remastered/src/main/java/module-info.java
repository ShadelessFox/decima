import com.shade.decima.game.hrzr.rtti.callbacks.*;
import com.shade.decima.rtti.generator.GenerateBindings;
import com.shade.decima.rtti.generator.GenerateBindings.Builtin;
import com.shade.decima.rtti.generator.GenerateBindings.Callback;

import java.math.BigInteger;

@GenerateBindings(
    source = "data/horizon_zero_dawn_remastered_rtti.json",
    target = "com.shade.decima.game.hrzr.rtti.HorizonZeroDawnRemastered",
    builtins = {
        @Builtin(type = "wchar", javaType = char.class),
        @Builtin(type = "int8", javaType = byte.class),
        @Builtin(type = "uint8", javaType = byte.class),
        @Builtin(type = "int16", javaType = short.class),
        @Builtin(type = "uint16", javaType = short.class),
        @Builtin(type = "int32", javaType = int.class),
        @Builtin(type = "uint32", javaType = int.class),
        @Builtin(type = "int", javaType = int.class),
        @Builtin(type = "uint", javaType = int.class),
        @Builtin(type = "int64", javaType = long.class),
        @Builtin(type = "uint64", javaType = long.class),
        @Builtin(type = "uintptr", javaType = long.class),
        @Builtin(type = "int128", javaType = BigInteger.class),
        @Builtin(type = "uint128", javaType = BigInteger.class),
        @Builtin(type = "float", javaType = float.class),
        @Builtin(type = "HalfFloat", javaType = float.class),
        @Builtin(type = "double", javaType = double.class),
        @Builtin(type = "bool", javaType = boolean.class),
        @Builtin(type = "String", javaType = String.class),
        @Builtin(type = "WString", javaType = String.class)
    },
    callbacks = {
        @Callback(type = "DataBufferResource", handler = DataBufferResourceCallback.class),
        @Callback(type = "IndexArrayResource", handler = IndexArrayResourceCallback.class),
        @Callback(type = "LocalizedSimpleSoundResource", handler = LocalizedSimpleSoundResourceCallback.class),
        @Callback(type = "LocalizedTextResource", handler = LocalizedTextResourceCallback.class),
        @Callback(type = "MorphemeAnimation", handler = MorphemeAnimationCallback.class),
        @Callback(type = "MorphemeAsset", handler = MorphemeAssetCallback.class),
        @Callback(type = "MorphemeNetworkDefResource", handler = MorphemeNetworkDefResourceCallback.class),
        @Callback(type = "MorphemeNetworkInstancePreInitializedData", handler = MorphemeNetworkInstancePreInitializedDataCallback.class),
        @Callback(type = "PhysicsHeightMapOffsetCollisionResource", handler = PhysicsHeightMapOffsetCollisionResourceCallback .class),
        @Callback(type = "PhysicsRagdollResource", handler = PhysicsRagdollResourceCallback .class),
        @Callback(type = "PhysicsShapeResource", handler = PhysicsShapeResourceCallback.class),
        @Callback(type = "Pose", handler = PoseCallback.class),
        @Callback(type = "ShaderResource", handler = ShaderResourceCallback.class),
        @Callback(type = "Texture", handler = TextureCallback.class),
        @Callback(type = "TextureList", handler = TextureListCallback.class),
        @Callback(type = "UITexture", handler = UITextureCallback.class),
        @Callback(type = "VertexArrayResource", handler = VertexArrayResourceCallback.class),
        @Callback(type = "WaveResource", handler = WaveResourceCallback.class),
    }
)

module decima.game.hrzr {
    requires static decima.rtti.generator;

    requires decima.rtti;
    requires platform.util;

    requires org.lz4.java;
    requires org.slf4j;

    opens com.shade.decima.game.hrzr.rtti to decima.rtti;
    opens com.shade.decima.game.hrzr.rtti.callbacks to decima.rtti;
}