package com.shade.decima.rtti;

// import com.shade.decima.model.rtti.HFW;

import com.shade.decima.model.rtti.HZD;
import com.shade.decima.model.rtti.gen.RTTI;
import com.shade.util.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Test {
    /*
    Ideas:
    - For MsgReadBinary, make it return an interface so the reader could make the proxy extend from it to allow casts
    - For adapters (e.g. for providing a generic view of a texture), the reader could find all adapters and make the proxy extend from them
     */

    public static void main(String[] args) throws Throwable {
        final Path file = Path.of("D:/Programming/decima/samples/greywolf.core");
        final ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);
        final TypeReader reader = new TypeReader();

        while (buffer.hasRemaining()) {
            final long hash = buffer.getLong();
            final int size = buffer.getInt();
            final ByteBuffer slice = buffer.slice(buffer.position(), size).order(ByteOrder.LITTLE_ENDIAN);

            final Class<?> type = findClass(HZD.class, hash);
            final Object object = reader.read(type, slice);
            System.out.println(object);

            buffer.position(buffer.position() + size);
        }

        /*HZD.MultiMeshResource resource = null;
        for (HZD.MultiMeshResourcePart part : resource.getMeshParts()) {
            final HZD.MeshResourceBase mesh = part.getMesh().get();
            if (mesh instanceof HZD.LodMeshResource lod) {
                for (HZD.LodMeshResourcePart lodPart : lod.getMeshMeshes()) {
                    final HZD.MeshResourceBase lodMesh = lodPart.getMesh().get();
                }
            }
        }

        HZD.AnimatedTexture texture = null;
        MyGenericTextureType genericTexture = texture instanceof MyGenericTextureType ? (MyGenericTextureType) texture : null;

        HZD.TextureSet textureSet = null;
        for (HZD.TextureSetEntry entry : textureSet.getEntries()) {
            final HZD.Resource texture = entry.getTexture().get();
            // do something
        }*/
    }

    private static final Map<Class<?>, Map<Long, Class<?>>> lookup = new HashMap<>();

    @NotNull
    private static Class<?> findClass(@NotNull Class<?> namespace, long hash) {
        final Map<Long, Class<?>> classes = lookup.computeIfAbsent(namespace, ns -> {
            final Map<Long, Class<?>> result = new HashMap<>();
            for (Class<?> cls : ns.getClasses()) {
                final RTTI.Serializable serializable = cls.getDeclaredAnnotation(RTTI.Serializable.class);
                if (serializable == null) {
                    continue;
                }
                result.put(serializable.hash(), cls);
            }
            return result;
        });
        final Class<?> cls = classes.get(hash);
        if (cls == null) {
            throw new NoSuchElementException("Can't find class with hash %#018x".formatted(hash));
        }
        return cls;
    }


}
