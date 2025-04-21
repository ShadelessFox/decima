package com.shade.decima.app;

import com.shade.decima.game.hfw.GraphWalker;
import com.shade.decima.game.hfw.game.ForbiddenWestGame;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.MeshResourceBase;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.PrimitiveResource;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.RegularSkinnedMeshResource;
import com.shade.decima.game.hfw.rtti.HorizonForbiddenWest.StaticMeshResource;
import com.shade.decima.rtti.data.Ref;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DumpForbiddenWestPrimitives {
    public static void main(String[] args) throws IOException {
        var source = Path.of("E:/SteamLibrary/steamapps/common/Horizon Forbidden West Complete Edition");
        var platform = HorizonForbiddenWest.EPlatform.WinGame;
        var game = new ForbiddenWestGame(source, platform);

        try (var writer = Files.newBufferedWriter(Path.of("samples/hfw/primitives.csv"))) {
            writer.write("MeshGroup,MeshIndex,NumIndices,NumVertices,NumStreams");

            for (var result : GraphWalker.iterate(MeshResourceBase.class, game, true)) {
                List<Ref<PrimitiveResource>> primitives;

                switch (result.object()) {
                    case StaticMeshResource r -> primitives = r.meshDescription().primitives();
                    case RegularSkinnedMeshResource r -> primitives = r.primitives();
                    default -> {
                        continue;
                    }
                }

                for (var primitive : Ref.unwrap(primitives)) {
                    var vertexArray = primitive.vertexArray().get();
                    var indexArray = primitive.indexArray().get();

                    writer.newLine();
                    writer.write("%d,%d,%d,%d,%d".formatted(
                        result.groupId(),
                        result.objectIndex(),
                        indexArray.count(),
                        vertexArray.count(),
                        vertexArray.streams().size()
                    ));
                }
            }
        }
    }
}
