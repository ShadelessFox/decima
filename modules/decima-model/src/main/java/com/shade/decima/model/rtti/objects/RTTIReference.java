package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.base.CoreBinary;
import com.shade.decima.model.packfile.Packfile;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;

public sealed interface RTTIReference permits RTTIReference.None, RTTIReference.Internal, RTTIReference.External {
    None NONE = new None();

    @Nullable
    FollowResult follow(@NotNull Project project, @NotNull CoreBinary current) throws IOException;

    @Nullable
    default RTTIObject get(@NotNull Project project, @NotNull CoreBinary current) throws IOException {
        final FollowResult result = follow(project, current);

        if (result == null) {
            return null;
        } else {
            return result.object;
        }
    }

    record External(@NotNull Kind kind, @NotNull RTTIObject uuid, @NotNull String path) implements RTTIReference {
        @NotNull
        @Override
        public FollowResult follow(@NotNull Project project, @NotNull CoreBinary current) throws IOException {
            final Packfile packfile = project.getPackfileManager().findFirst(path);

            if (packfile == null) {
                throw new IOException("Couldn't find referenced file: " + path);
            }

            final CoreBinary binary = CoreBinary.from(packfile.extract(path), project.getTypeRegistry(), true);
            final RTTIObject object = binary.find(uuid);

            if (object == null) {
                throw new IOException("Couldn't find referenced entry: " + RTTIUtils.uuidToString(uuid));
            }

            return new FollowResult(binary, object);
        }
    }

    record Internal(@NotNull Kind kind, @NotNull RTTIObject uuid) implements RTTIReference {
        @NotNull
        @Override
        public FollowResult follow(@NotNull Project project, @NotNull CoreBinary current) throws IOException {
            final RTTIObject object = current.find(uuid);

            if (object == null) {
                throw new IOException("Couldn't find referenced entry: " + RTTIUtils.uuidToString(uuid));
            }

            return new FollowResult(current, object);
        }
    }

    record None() implements RTTIReference {
        @Nullable
        @Override
        public FollowResult follow(@NotNull Project project, @NotNull CoreBinary current) {
            return null;
        }
    }

    enum Kind {
        LINK,
        REFERENCE
    }

    record FollowResult(@NotNull CoreBinary binary, @NotNull RTTIObject object) {}
}
