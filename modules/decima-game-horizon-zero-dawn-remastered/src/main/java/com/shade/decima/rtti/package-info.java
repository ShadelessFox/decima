@GenerateBindings(
    namespace = "HorizonZeroDawnRemastered",
    source = "data/horizon_zero_dawn_remastered_rtti.json",
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
    }
)
package com.shade.decima.rtti;

import com.shade.decima.rtti.generator.GenerateBindings;
import com.shade.decima.rtti.generator.GenerateBindings.Builtin;

import java.math.BigInteger;