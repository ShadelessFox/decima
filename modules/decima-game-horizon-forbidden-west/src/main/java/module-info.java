import com.shade.decima.game.hfw.rtti.callbacks.TextureCallback;
import com.shade.decima.game.hfw.rtti.callbacks.UITextureCallback;
import com.shade.decima.game.hfw.rtti.callbacks.UITextureFramesCallback;
import com.shade.decima.rtti.generator.GenerateBindings;
import com.shade.decima.rtti.generator.GenerateBindings.Builtin;
import com.shade.decima.rtti.generator.GenerateBindings.Callback;

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
        @Callback(type = "Texture", handler = TextureCallback.class),
        @Callback(type = "UITexture", handler = UITextureCallback.class),
        @Callback(type = "UITextureFrames", handler = UITextureFramesCallback.class),
    }
)
module decima.game.hfw {
    requires static decima.rtti.generator;

    requires decima.rtti;
    requires decima.game;

    requires org.slf4j;

    opens com.shade.decima.game.hfw.rtti to decima.rtti;
    opens com.shade.decima.game.hfw.rtti.callbacks to decima.rtti;
}