import com.shade.decima.game.killzone3.rtti.callbacks.*;
import com.shade.decima.rtti.generator.GenerateBindings;
import com.shade.decima.rtti.generator.GenerateBindings.Builtin;
import com.shade.decima.rtti.generator.GenerateBindings.Callback;

import java.math.BigInteger;

@GenerateBindings(
    source = "data/killzone3_rtti.json",
    target = "com.shade.decima.game.killzone3.rtti.Killzone3",
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
        @Builtin(type = "HalfFloat", javaType = float.class),
        @Builtin(type = "float", javaType = float.class),
        @Builtin(type = "double", javaType = double.class),
        @Builtin(type = "bool", javaType = boolean.class),
        @Builtin(type = "String", javaType = String.class),
        @Builtin(type = "WString", javaType = String.class)
    },
    callbacks = {
        @Callback(type = "AIDynamicWaypointGrid", handler = AIDynamicWaypointGridCallback.class),
        @Callback(type = "IndexArrayResource", handler = IndexArrayResourceCallback.class),
        @Callback(type = "PhysicsCollisionResource", handler = PhysicsCollisionResourceCallback.class),
        @Callback(type = "Pose", handler = PoseCallback.class),
        @Callback(type = "ShaderResource", handler = ShaderResourceCallback.class),
        @Callback(type = "SkinnedMeshBoneBindings", handler = SkinnedMeshBoneBindingsCallback.class),
        @Callback(type = "SoundBankResource", handler = SoundBankResourceCallback.class),
        @Callback(type = "StaticMeshInstance", handler = StaticMeshInstanceCallback.class),
        @Callback(type = "Texture", handler = TextureCallback.class),
        @Callback(type = "VertexArrayResource", handler = VertexArrayResourceCallback .class),
    }
)
module decima.game.killzone3 {
    requires static decima.rtti.generator;

    requires decima.rtti;
    requires platform.util;

    requires org.slf4j;

    opens com.shade.decima.game.killzone3.rtti to decima.rtti;
    opens com.shade.decima.game.killzone3.rtti.callbacks to decima.rtti;
    opens com.shade.decima.game.killzone3.rtti.data to decima.rtti;

    exports com.shade.decima.game.killzone3.rtti;
    exports com.shade.decima.game.killzone3.rtti.callbacks;
    exports com.shade.decima.game.killzone3.rtti.data;
}
