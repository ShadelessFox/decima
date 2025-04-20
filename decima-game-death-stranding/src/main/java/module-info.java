import com.shade.decima.rtti.generator.GenerateBindings;

import java.math.BigInteger;

@GenerateBindings(
    source = "data/death_stranding_rtti.json",
    target = "com.shade.decima.game.ds.rtti.DeathStranding",
    builtins = {
        @GenerateBindings.Builtin(type = "wchar", javaType = char.class),
        @GenerateBindings.Builtin(type = "ucs4", javaType = int.class),
        @GenerateBindings.Builtin(type = "int8", javaType = byte.class),
        @GenerateBindings.Builtin(type = "uint8", javaType = byte.class),
        @GenerateBindings.Builtin(type = "int16", javaType = short.class),
        @GenerateBindings.Builtin(type = "uint16", javaType = short.class),
        @GenerateBindings.Builtin(type = "int32", javaType = int.class),
        @GenerateBindings.Builtin(type = "uint32", javaType = int.class),
        @GenerateBindings.Builtin(type = "int", javaType = int.class),
        @GenerateBindings.Builtin(type = "uint", javaType = int.class),
        @GenerateBindings.Builtin(type = "int64", javaType = long.class),
        @GenerateBindings.Builtin(type = "uint64", javaType = long.class),
        @GenerateBindings.Builtin(type = "uint128", javaType = BigInteger.class),
        @GenerateBindings.Builtin(type = "float", javaType = float.class),
        @GenerateBindings.Builtin(type = "HalfFloat", javaType = float.class),
        @GenerateBindings.Builtin(type = "double", javaType = double.class),
        @GenerateBindings.Builtin(type = "bool", javaType = boolean.class),
        @GenerateBindings.Builtin(type = "String", javaType = String.class),
        @GenerateBindings.Builtin(type = "WString", javaType = String.class)
    }
)

module decima.game.death_stranding {
    requires static decima.rtti.generator;

    requires decima.core;
    requires decima.rtti;
}
