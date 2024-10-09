package com.shade.decima.rtti;

import com.shade.util.NotNull;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.shade.decima.rtti.UntilDawn.*;
import static org.junit.jupiter.api.Assertions.*;

class RTTITest {
    @ParameterizedTest
    @MethodSource("getTypes")
    void canConstructAndPassEqualityCheck(@NotNull Class<?> iface) {
        var instance = RTTI.newInstance(iface);
        var representation = instance.getClass();
        var categories = RTTI.getCategories(iface).stream()
            .map(RTTI.CategoryInfo::name)
            .toArray(String[]::new);

        EqualsVerifier.forClass(representation)
            .suppress(Warning.NONFINAL_FIELDS)
            .withIgnoredFields(categories)
            .verify();
    }

    @Test
    void canProperlySortAttributes() {
        List<RTTI.AttributeInfo> attrs;

        // This type has a particularly weird sorting due to the fact that
        // all its attributes are properties and don't have an offset, so
        // the test ensures that they get sorted correctly using a dedicated
        // algorithm used internally by the engine.
        attrs = RTTI.getAttrsSorted(RenderPass.class);
        assertEquals(10, attrs.size());
        assertEquals("HasAlphaTest", attrs.get(0).name());
        assertEquals("Wireframe", attrs.get(1).name());
        assertEquals("EnableDepthClamp", attrs.get(2).name());
        assertEquals("ColorMask", attrs.get(3).name());
        assertEquals("DepthBias", attrs.get(4).name());
        assertEquals("UseStencil", attrs.get(5).name());
        assertEquals("BlendMode", attrs.get(6).name());
        assertEquals("WriteDepth", attrs.get(7).name());
        assertEquals("CullMode", attrs.get(8).name());
        assertEquals("DepthTestFunc", attrs.get(9).name());

        // And for other types, the order should be by offset of their attributes
        attrs = RTTI.getAttrsSorted(Vec3.class);
        assertEquals(3, attrs.size());
        assertEquals("X", attrs.get(0).name());
        assertEquals("Y", attrs.get(1).name());
        assertEquals("Z", attrs.get(2).name());
    }

    @Test
    void canGetAndSetAttributes() {
        Vec3 vec3 = RTTI.newInstance(Vec3.class);
        assertEquals(0, vec3.x());
        assertEquals(0, vec3.y());
        assertEquals(0, vec3.z());

        assertDoesNotThrow(() -> vec3.x(1));
        assertEquals(1, vec3.x());
        assertEquals(0, vec3.y());
        assertEquals(0, vec3.z());

        assertDoesNotThrow(() -> vec3.y(2f));
        assertEquals(1, vec3.x());
        assertEquals(2, vec3.y());
        assertEquals(0, vec3.z());

        assertDoesNotThrow(() -> vec3.z(3f));
        assertEquals(1, vec3.x());
        assertEquals(2, vec3.y());
        assertEquals(3, vec3.z());
    }

    @NotNull
    private static Stream<Class<?>> getTypes() {
        return RTTI.getTypes(UntilDawn.class).stream()
            .filter(Predicate.not(Class::isEnum));
    }
}
