import com.shade.decima.rtti.generator.GenerateBindings;

import java.math.BigInteger;

@GenerateBindings(
    source = "data/horizon_forbidden_west_rtti.json",
    target = "com.shade.decima.game.hfw.rtti.HorizonForbiddenWest",
    builtins = {
        @GenerateBindings.Builtin(type = "bool", javaType = boolean.class),
        @GenerateBindings.Builtin(type = "int", javaType = int.class),
        @GenerateBindings.Builtin(type = "int8", javaType = byte.class),
        @GenerateBindings.Builtin(type = "int16", javaType = short.class),
        @GenerateBindings.Builtin(type = "int32", javaType = int.class),
        @GenerateBindings.Builtin(type = "int64", javaType = long.class),
        @GenerateBindings.Builtin(type = "intptr", javaType = long.class),
        @GenerateBindings.Builtin(type = "uint", javaType = int.class),
        @GenerateBindings.Builtin(type = "uint8", javaType = byte.class),
        @GenerateBindings.Builtin(type = "uint16", javaType = short.class),
        @GenerateBindings.Builtin(type = "uint32", javaType = int.class),
        @GenerateBindings.Builtin(type = "uint64", javaType = long.class),
        @GenerateBindings.Builtin(type = "uint128", javaType = BigInteger.class),
        @GenerateBindings.Builtin(type = "uintptr", javaType = long.class),
        @GenerateBindings.Builtin(type = "float", javaType = float.class),
        @GenerateBindings.Builtin(type = "double", javaType = double.class),
        @GenerateBindings.Builtin(type = "HalfFloat", javaType = float.class),
        @GenerateBindings.Builtin(type = "tchar", javaType = char.class),
        @GenerateBindings.Builtin(type = "wchar", javaType = char.class),
        @GenerateBindings.Builtin(type = "ucs4", javaType = int.class),
        @GenerateBindings.Builtin(type = "String", javaType = String.class),
        @GenerateBindings.Builtin(type = "WString", javaType = String.class),
    },
    callbacks = {

    }
)
module decima.game.hfw {
    requires static decima.rtti.generator;

    requires decima.rtti;
    requires decima.game;

    requires org.slf4j;

    opens com.shade.decima.game.hfw.rtti to decima.rtti;
    // opens com.shade.decima.game.hfw.rtti.callbacks to decima.rtti;
}