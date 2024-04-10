package com.shade.decima.model.rtti.objects;

import com.shade.decima.model.app.Project;
import com.shade.decima.model.archive.ArchiveFile;
import com.shade.decima.model.rtti.RTTICoreFile;
import com.shade.decima.model.rtti.RTTIUtils;
import com.shade.util.NotImplementedException;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.io.IOException;

public sealed interface RTTIReference {
    None NONE = new None();

    @Nullable
    FollowResult follow(@NotNull Project project, @NotNull RTTICoreFile current) throws IOException;

    @Nullable
    default RTTIObject get(@NotNull Project project, @NotNull RTTICoreFile current) throws IOException {
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
        public FollowResult follow(@NotNull Project project, @NotNull RTTICoreFile current) throws IOException {
            final ArchiveFile file = project.getPackfileManager().getFile(path);
            final RTTICoreFile core = project.getCoreFileReader().read(file, true);
            return Internal.follow(core, uuid);
        }

        @Override
        public String toString() {
            return "<external " + kind + " to " + path + ':' + RTTIUtils.uuidToString(uuid) + ">";
        }
    }

    record Internal(@NotNull Kind kind, @NotNull RTTIObject uuid) implements RTTIReference {
        @NotNull
        @Override
        public FollowResult follow(@NotNull Project project, @NotNull RTTICoreFile current) throws IOException {
            return follow(current);
        }

        @NotNull
        public FollowResult follow(@NotNull RTTICoreFile current) throws IOException {
            return follow(current, uuid);
        }

        @NotNull
        private static FollowResult follow(@NotNull RTTICoreFile current, @NotNull RTTIObject uuid) throws IOException {
            for (RTTIObject object : current.objects()) {
                if (object.uuid().equals(uuid)) {
                    return new FollowResult(current, object);
                }
            }

            throw new IOException("Couldn't find referenced object: " + RTTIUtils.uuidToString(uuid));
        }

        @Override
        public String toString() {
            return "<internal " + kind + " to " + RTTIUtils.uuidToString(uuid) + ">";
        }
    }

    final class StreamingLink implements RTTIReference {
        private RTTIObject target;

        public StreamingLink(@Nullable RTTIObject target) {
            this.target = target;
        }

        @Override
        public FollowResult follow(@NotNull Project project, @NotNull RTTICoreFile current) {
            throw new NotImplementedException();
        }

        @Nullable
        public RTTIObject getTarget() {
            return target;
        }

        public void setTarget(@Nullable RTTIObject target) {
            this.target = target;
        }

        @Override
        public String toString() {
            return "<link to " + target + ">";
        }
    }

    record None() implements RTTIReference {
        @Nullable
        @Override
        public FollowResult follow(@NotNull Project project, @NotNull RTTICoreFile current) {
            return null;
        }

        @Override
        public String toString() {
            return "<null reference>";
        }
    }

    enum Kind {
        LINK,
        REFERENCE;

        @Override
        public String toString() {
            return switch (this) {
                case LINK -> "link";
                case REFERENCE -> "reference";
            };
        }
    }

    record FollowResult(@NotNull RTTICoreFile file, @NotNull RTTIObject object) {}
}
