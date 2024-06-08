package com.shade.decima.model.rtti.gen;

import com.shade.util.NotNull;

import java.io.Flushable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.invoke.MethodType;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypeGenerator implements Flushable {
    private static final Map<String, Class<?>> NATIVE_TYPES = Map.ofEntries(
        Map.entry("uint", int.class),
        Map.entry("uintptr", long.class),
        Map.entry("uint8", byte.class),
        Map.entry("uint16", short.class),
        Map.entry("uint32", int.class),
        Map.entry("uint64", long.class),
        Map.entry("uint128", BigInteger.class),

        Map.entry("int", int.class),
        Map.entry("intptr", long.class),
        Map.entry("int8", byte.class),
        Map.entry("int16", short.class),
        Map.entry("int32", int.class),
        Map.entry("int64", long.class),
        Map.entry("int128", BigInteger.class),

        Map.entry("tchar", byte.class),
        Map.entry("wchar", short.class),
        Map.entry("ucs4", int.class),

        Map.entry("float", float.class),
        Map.entry("double", double.class),
        Map.entry("bool", boolean.class),

        Map.entry("HalfFloat", float.class),
        Map.entry("String", String.class),
        Map.entry("WString", String.class)
    );

    public static void main(String[] args) throws Exception {
        final Path source = Path.of("D:/Programming/decima/data/hzd_types.json");
        final TypeContext context = new TypeContext();
        context.load(source);

        final TypeGenerator generator = new TypeGenerator(new PrintWriter(System.out), 0);
        generator.generate(context);
        generator.flush();
    }

    private final Writer writer;
    private final int indent;

    public TypeGenerator(@NotNull Writer writer, int indent) {
        this.writer = writer;
        this.indent = indent;
    }

    public void generate(@NotNull TypeContext context) throws IOException {
        for (Iterator<TypeContext.Type> it = context.types().iterator(); it.hasNext(); ) {
            final TypeContext.Type type = it.next();

            if (type instanceof TypeContext.ClassType classType) {
                generateClass(classType);
            } else if (type instanceof TypeContext.EnumType enumType) {
                generateEnum(enumType);
            } else if (type instanceof TypeContext.AtomType atomType && atomType.base() != null) {
                generateAtom(atomType);
            } else {
                continue;
            }

            if (it.hasNext()) {
                newline();
            }
        }
    }

    public void generateClass(@NotNull TypeContext.ClassType type) throws IOException {
        final long hash = new TypeHasher().getTypeId(type).low();

        writeLn("@RTTI.Serializable(hash = %#18xL)".formatted(hash));
        write("public interface %s".formatted(type.name()));

        if (!type.bases().isEmpty()) {
            writer.write(" extends ");
            for (int i = 0; i < type.bases().size(); i++) {
                final TypeContext.ClassBase base = type.bases().get(i);
                writer.write("@RTTI.Base(offset = %d) %s".formatted(base.offset(), base.type().name()));
                if (i < type.bases().size() - 1) {
                    writer.write(", ");
                }
            }
        }

        writer.write(" {");

        if (!type.attrs().isEmpty()) {
            for (int i = 0; i < type.attrs().size(); i++) {
                final TypeContext.ClassAttr attr = type.attrs().get(i);
                lnWrite("@RTTI.Attr(", indent + 1);
                if (attr.type().get() instanceof TypeContext.AtomType) {
                    writer.write("type = \"%s\", ".formatted(attr.type().name()));
                }
                if (attr.category() != null) {
                    writer.write("category = \"%s\", ".formatted(attr.category()));
                }
                writer.write("offset = %d, flags = %d)".formatted(attr.offset(), attr.flags()));
                lnWrite("%s %s(); // %s".formatted(getTypeName(attr.type(), false), getGetterName(attr), attr.name()), indent + 1);
                if (i < type.attrs().size() - 1) {
                    newline();
                }
            }
        }

        lnWrite("}");
        newline();
    }

    public void generateEnum(@NotNull TypeContext.EnumType type) throws IOException {
        final String base = switch (type.size()) {
            case 1 -> "RTTI.Enum.OfByte";
            case 2 -> "RTTI.Enum.OfShort";
            case 4 -> "RTTI.Enum.OfInt";
            default -> throw new IllegalArgumentException("Unsupported enum size: " + type.size());
        };
        final String storage = switch (type.size()) {
            case 1 -> "byte";
            case 2 -> "short";
            default -> "int";
        };
        final String cast = switch (type.size()) {
            case 1 -> "(byte) ";
            case 2 -> "(short) ";
            default -> "";
        };

        write("public enum %s implements %s {".formatted(type.name(), base));

        for (int i = 0; i < type.values().size(); i++) {
            final TypeContext.EnumValue value = type.values().get(i);
            lnWrite("_%d(%d, \"%s\"".formatted(i, value.value(), escapeStringLiteral(value.name())), indent + 1);
            for (String alias : value.aliases()) {
                writer.write(", \"%s\"".formatted(escapeStringLiteral(alias)));
            }
            writer.write(")");
            writer.write(i < type.values().size() - 1 ? "," : ";");
        }

        newline();
        newline();
        writeLn("private final int value;", indent + 1);
        writeLn("private final String name;", indent + 1);
        writeLn("private final String[] aliases;", indent + 1);

        newline();
        writeLn("%s(int value, String name, String... aliases) {".formatted(type.name()), indent + 1);
        writeLn("this.value = value;", indent + 2);
        writeLn("this.name = name;", indent + 2);
        writeLn("this.aliases = aliases;", indent + 2);
        writeLn("}", indent + 1);

        newline();
        writeLn("@Override", indent + 1);
        writeLn("public %s value() {".formatted(storage), indent + 1);
        writeLn("return %svalue;".formatted(cast), indent + 2);
        writeLn("}", indent + 1);

        newline();
        writeLn("@Override", indent + 1);
        writeLn("public String[] aliases() {", indent + 1);
        writeLn("return aliases;", indent + 2);
        writeLn("}", indent + 1);

        newline();
        writeLn("@Override", indent + 1);
        writeLn("public String toString() {", indent + 1);
        writeLn("return name;", indent + 2);
        writeLn("}", indent + 1);

        writeLn("}");
    }

    public void generateAtom(@NotNull TypeContext.AtomType type) throws IOException {
        writeLn("public record %s(%s value) {".formatted(type.name(), getTypeName(Objects.requireNonNull(type.base()), false)));

        writeLn("@Override", indent + 1);
        writeLn("public String toString() {", indent + 1);
        writeLn("return String.valueOf(value);", indent + 2);
        writeLn("}", indent + 1);

        writeLn("}");
    }

    private static final Pattern HUNGARIAN_CASE = Pattern.compile("^m[A-Z]\\w+$");
    private static final Pattern LOWER_SNAKE_CASE = Pattern.compile("^[a-z][a-z0-9]*(_[a-z0-9]+)+$");
    private static final Pattern UPPER_SNAKE_CASE = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)+$");
    private static final Pattern IS_BOOLEAN = Pattern.compile("^(Get|Is|Has)([A-Z0-9][A-Za-z0-9]*)$");

    @NotNull
    private static String escapeStringLiteral(@NotNull String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @NotNull
    private static String getGetterName(@NotNull TypeContext.ClassAttr attr) {
        final String normalized = toCamelCase(attr.name());

        final String prefix;
        final String name;

        final Matcher matcher = IS_BOOLEAN.matcher(normalized);
        if (matcher.matches()) {
            prefix = matcher.group(1);
            name = matcher.group(2);
        } else if (attr.type().name().equals("bool")) {
            prefix = "is";
            name = normalized;
        } else {
            prefix = "get";
            name = normalized;
        }

        final String category;
        if (attr.category() == null || attr.category().equals("General")) {
            category = null;
        } else {
            category = attr.category();
        }

        if (category != null) {
            return prefix + category + name;
        } else {
            return prefix + name;
        }
    }

    @NotNull
    private static String toCamelCase(@NotNull String name) {
        if (HUNGARIAN_CASE.matcher(name).matches()) {
            return name.substring(1);
        }
        if (LOWER_SNAKE_CASE.matcher(name).matches()) {
            return Arrays.stream(name.split("_"))
                .map(TypeGenerator::capitalize)
                .reduce("", String::concat);
        }
        if (UPPER_SNAKE_CASE.matcher(name).matches()) {
            return Arrays.stream(name.split("_"))
                .map(String::toLowerCase)
                .map(TypeGenerator::capitalize)
                .reduce("", String::concat);
        }
        return capitalize(name);
    }

    @NotNull
    private static String capitalize(@NotNull String name) {
        final String upper = name.toUpperCase();
        final String lower = name.toLowerCase();
        if (upper.equals("UUID") || upper.equals("ID")) {
            return upper;
        } else if (upper.length() > 3 && upper.equals(name)) {
            return upper.charAt(0) + lower.substring(1);
        } else {
            return upper.charAt(0) + name.substring(1);
        }
    }

    @NotNull
    private static String getTypeName(@NotNull TypeContext.TypeRef<?> ref, boolean boxed) {
        final Class<?> cls = NATIVE_TYPES.get(ref.name());
        if (cls != null) {
            if (boxed && cls.isPrimitive()) {
                return MethodType.methodType(cls).wrap().returnType().getSimpleName();
            } else {
                return cls.getSimpleName();
            }
        }
        final TypeContext.Type type = ref.get();
        if (type instanceof TypeContext.TemplateType template) {
            final String name = template.type().name();
            final String argument = getTypeName(template.argument(), true);
            return switch (name) {
                case "Array", "HashMap", "HashSet" -> "List<%s>".formatted(argument);
                case "Ref", "UUIDRef", "StreamingRef", "WeakPtr", "cptr" -> "Ref<%s>".formatted(argument);
                default -> throw new IllegalArgumentException("Unsupported template type: " + name);
            };
        }
        if (type instanceof TypeContext.EnumType enumType && enumType.flags()) {
            return "EnumSet<%s>".formatted(type.name());
        }
        return type.name();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    private void lnWrite(@NotNull String text) throws IOException {
        newline();
        write(text);
    }

    private void lnWrite(@NotNull String text, int indent) throws IOException {
        newline();
        write(text, indent);
    }

    private void writeLn(@NotNull String text) throws IOException {
        write(text);
        newline();
    }

    private void writeLn(@NotNull String text, int indent) throws IOException {
        write(text, indent);
        newline();
    }

    private void write(@NotNull String text) throws IOException {
        write(text, indent);
    }

    private void write(@NotNull String text, int indent) throws IOException {
        indent(indent);
        writer.write(text);
    }

    private void indent(int indent) throws IOException {
        writer.write("\t".repeat(indent));
    }

    private void newline() throws IOException {
        writer.write(System.lineSeparator());
    }
}
