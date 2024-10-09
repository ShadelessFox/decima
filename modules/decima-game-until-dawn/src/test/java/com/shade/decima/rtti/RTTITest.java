package com.shade.decima.rtti;

import com.shade.util.NotNull;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.shade.decima.rtti.UntilDawn.Vec3;
import static org.junit.jupiter.api.Assertions.*;

class RTTITest {
    @ParameterizedTest()
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
