package com.shade.decima.app.tools;

import com.shade.decima.game.until_dawn.game.UntilDawnGame;
import com.shade.decima.game.until_dawn.rtti.UntilDawn;
import com.shade.decima.game.until_dawn.rtti.UntilDawn.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class UntilDawnVoiceLinesExporter {
    public static void main(String[] args) throws Exception {
        var game = new UntilDawnGame(Path.of("D:/PlayStation Games/Until Dawn"), EPlatform.PINK);
        var output = Path.of("samples/until_dawn/voicelines");

        var sections = game.loadObjectSystem("cache:lumps/assets_description.levellist_concreteasset.core").stream()
            .filter(LevelSection.class::isInstance)
            .map(LevelSection.class::cast)
            .toList();

        var texts = new HashMap<String, String>();
        var sources = new HashMap<String, byte[]>();

        for (LevelSection section : sections) {
            var lumps = section.lumps().stream()
                .filter(lump -> lump.locale() == ELanguage.Russian)
                .toList();

            for (LevelSectionLump lump : lumps) {
                var asset = (ConcreteAsset) lump.asset().get();
                var objects = game.loadObjectSystem(game.pathForLumpLocation(asset.lumpLocation()));

                for (Object object : objects) {
                    if (object instanceof UntilDawn.CommunicationMessageEventSound event) {
                        texts.put(event.general().soundName(), event.general().subtitle());
                    } else if (object instanceof UntilDawn.ExternalSourceCacheResource cache) {
                        for (var source : cache.sources()) {
                            sources.put(source.name(), source.data());
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, byte[]> source : sources.entrySet()) {
            Path wem = output.resolve("%s.wem".formatted(source.getKey()));
            Files.write(wem, source.getValue());
        }

        var mappings = texts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> "%s -> %s".formatted(entry.getKey(), entry.getValue()))
            .toList();

        Files.write(output.resolve("mappings.txt"), mappings);
    }
}
