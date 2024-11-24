package com.shade.decima.rtti;

import com.shade.decima.rtti.factory.TypeFactory;
import com.shade.decima.rtti.runtime.ClassAttrInfo;
import com.shade.decima.rtti.test.UntilDawnTypeFactory;
import com.shade.decima.rtti.test.UntilDawnTypeId;
import com.shade.util.NotNull;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static com.shade.decima.rtti.UntilDawn.*;
import static org.junit.jupiter.api.Assertions.*;

class RTTITest {
    private static final TypeFactory factory = new UntilDawnTypeFactory();

    @ParameterizedTest
    @MethodSource("getTypes")
    void canFindAndConstructAndPassEqualityCheck(@NotNull String name) {
        var info = assertDoesNotThrow(() -> factory.get(UntilDawnTypeId.of(name)), "Can find type");
        var instance = assertDoesNotThrow(info::newInstance, "Can construct type");
        var representation = instance.getClass();

        EqualsVerifier.forClass(representation)
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }

    @Test
    void canProperlySortAttributes() {
        List<ClassAttrInfo> attrs;

        // This type has a particularly weird sorting due to the fact that
        // all its attributes are properties and don't have an offset, so
        // the test ensures that they get sorted correctly using a dedicated
        // algorithm used internally by the engine.
        attrs = factory.get(RenderPass.class).serializableAttrs();
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
        attrs = factory.get(Vec3.class).serializableAttrs();
        assertEquals(3, attrs.size());
        assertEquals("X", attrs.get(0).name());
        assertEquals("Y", attrs.get(1).name());
        assertEquals("Z", attrs.get(2).name());
    }

    @Test
    void canGetAndSetAttributes() {
        Vec3 vec3 = factory.newInstance(Vec3.class);
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

    @Test
    void canToString() {
        assertEquals("UntilDawn$Vec3$POD[X=0.0, Y=0.0, Z=0.0]", factory.newInstance(Vec3.class).toString());
    }

    @Test
    void canCreateMultipleFactories() {
        UntilDawnTypeFactory factory1 = new UntilDawnTypeFactory();
        UntilDawnTypeFactory factory2 = new UntilDawnTypeFactory();

        assertNotEquals(factory1.get(Vec3.class).type(), factory2.get(Vec3.class).type());
    }

    @NotNull
    private static Stream<String> getTypes() {
        return Stream.of(UntilDawn.class.getDeclaredClasses())
            .filter(Class::isInterface)
            .map(Class::getSimpleName);
    }
}
