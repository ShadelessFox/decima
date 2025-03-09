import com.shade.decima.game.until_dawn.rtti.callbacks.*;
import com.shade.decima.rtti.generator.GenerateBindings;
import com.shade.decima.rtti.generator.GenerateBindings.Builtin;
import com.shade.decima.rtti.generator.GenerateBindings.Callback;

import java.math.BigInteger;

@GenerateBindings(
    source = "data/until_dawn_rtti.json",
    target = "com.shade.decima.game.until_dawn.rtti.UntilDawn",
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
        @Builtin(type = "int128", javaType = BigInteger.class),
        @Builtin(type = "uint128", javaType = BigInteger.class),
        @Builtin(type = "float", javaType = float.class),
        @Builtin(type = "double", javaType = double.class),
        @Builtin(type = "bool", javaType = boolean.class),
        @Builtin(type = "String", javaType = String.class),
        @Builtin(type = "WString", javaType = String.class)
    },
    callbacks = {
        @Callback(type = "AnimatedTexture", handler = AnimatedTextureCallback.class),
        @Callback(type = "CoreScript", handler = CoreScriptCallback.class),
        @Callback(type = "ExternalSourceCacheResource", handler = ExternalSourceCacheResourceCallback.class),
        @Callback(type = "HavokClothResource", handler = HavokClothResourceCallback.class),
        @Callback(type = "IndexArrayResource", handler = IndexArrayResourceCallback.class),
        @Callback(type = "MorphemeAnimationResource", handler = MorphemeAnimationResourceCallback.class),
        @Callback(type = "NavMesh", handler = NavMeshCallback.class),
        @Callback(type = "PhysicsCollisionResource", handler = PhysicsCollisionResourceCallback.class),
        @Callback(type = "ScaleformGFxMovieResource", handler = ScaleformGFxMovieResourceCallback.class),
        @Callback(type = "ShaderResource", handler = ShaderResourceCallback.class),
        @Callback(type = "Texture", handler = TextureCallback.class),
        @Callback(type = "VertexArrayResource", handler = VertexArrayResourceCallback.class),
        @Callback(type = "WWiseSoundBankResource", handler = WWiseSoundBankResourceCallback.class),
    }
)
module decima.game.until_dawn {
    requires static decima.rtti.generator;

    requires decima.rtti;
    requires platform.util;

    requires org.lz4.java;
    requires org.slf4j;

    opens com.shade.decima.game.until_dawn.rtti to decima.rtti;
    opens com.shade.decima.game.until_dawn.rtti.data to decima.rtti;
    opens com.shade.decima.game.until_dawn.rtti.callbacks to decima.rtti;

    exports com.shade.decima.game.until_dawn.rtti;
}
