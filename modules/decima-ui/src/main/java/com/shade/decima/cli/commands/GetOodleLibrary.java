package com.shade.decima.cli.commands;

import com.shade.platform.model.util.IOUtils;
import com.shade.util.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

import static picocli.CommandLine.Help.Visibility.ALWAYS;

@Command(name = "get-oodle", description = """
    Downloads Oodle compression library for the platform specified.
        
    Instruction:
    1. Follow the provided instructions to access the Unreal Engine repository: https://github.com/EpicGames/Signup
    2. Download the following file: https://github.com/EpicGames/UnrealEngine/blob/release/Engine/Build/Commit.gitdeps.xml
    3. Use that downloaded file as an argument to this command
    """, sortOptions = false)
public class GetOodleLibrary implements Callable<Void> {
    public enum Platform {
        WINDOWS_64 {
            @Override
            protected boolean matches(@NotNull String name) {
                return name.startsWith("oo2core") && name.endsWith("win64.dll");
            }
        },
        LINUX_64 {
            @Override
            protected boolean matches(@NotNull String name) {
                return name.startsWith("liboo2corelinux64.so");
            }
        },
        DARWIN_64 {
            @Override
            protected boolean matches(@NotNull String name) {
                return name.startsWith("liboo2coremac64.") && name.endsWith(".dylib");
            }
        };

        protected abstract boolean matches(@NotNull String name);
    }

    @Parameters(index = "0", description = "Path to the 'Commit.gitdeps.xml' file")
    private Path manifestPath;

    @Option(names = {"-p", "--platform"}, description = "The target platform. Valid values: ${COMPLETION-CANDIDATES}", showDefaultValue = ALWAYS)
    private Platform platform = Platform.WINDOWS_64;

    @Option(names = {"-o", "--output"}, description = "The output directory", showDefaultValue = ALWAYS)
    private Path outputPath = Path.of("").toAbsolutePath();

    @Option(names = {"-d", "--default"}, description = "Use default choice or prompt if multiple files were found", showDefaultValue = ALWAYS)
    private boolean prompt = true;

    @Override
    public Void call() throws Exception {
        final DependencyManifest manifest;

        try (InputStream is = new BufferedInputStream(Files.newInputStream(manifestPath))) {
            manifest = DependencyManifest.parse(is);
        }

        final DependencyManifest.File[] files = manifest.files.stream()
            .filter(file -> platform.matches(IOUtils.getFilename(file.name)))
            .toArray(DependencyManifest.File[]::new);

        if (files.length == 0) {
            System.out.println("No matching files were found");
            return null;
        }

        int index = files.length - 1;

        if (files.length > 1 && prompt) {
            System.out.println("Multiple matching files were found. Choose one:");

            for (int i = 0; i < files.length; i++) {
                System.out.printf("%d) %s%s%n", i + 1, files[i].name, i == files.length - 1 ? " (default)" : "");
            }

            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("> ");

                    final String choice = scanner.nextLine();

                    if (!choice.isEmpty()) {
                        try {
                            index = Math.clamp(Integer.parseInt(choice) - 1, 0, files.length - 1);
                        } catch (NumberFormatException ignored) {
                            continue;
                        }
                    }

                    break;
                }
            }
        }

        final DependencyManifest.File file = files[index];
        final DependencyManifest.Blob blob = manifest.blobs.stream()
            .filter(b -> b.hash.equals(file.hash))
            .findFirst().orElseThrow(() -> new IllegalStateException("Can't find blob containing file " + file));
        final DependencyManifest.Pack pack = manifest.packs.stream()
            .filter(p -> p.hash.equals(blob.packHash))
            .findFirst().orElseThrow(() -> new IllegalStateException("Can't find pack containing blob " + blob));

        System.out.println("Downloading file " + file.name);

        final HttpClient client = HttpClient.newHttpClient();
        final HttpResponse<InputStream> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("%s/%s/%s".formatted(manifest.baseUrl, pack.remotePath, pack.hash)))
                .build(),
            BodyHandlers.ofInputStream()
        );

        if (response.statusCode() != 200) {
            System.out.println("Unexpected status code: " + response.statusCode());
        }

        final byte[] data = new byte[(int) blob.size];

        try (InputStream is = new GZIPInputStream(response.body())) {
            is.skipNBytes(blob.packOffset);
            is.readNBytes(data, 0, data.length);
        }

        final Path path = outputPath.resolve(IOUtils.getFilename(file.name));
        Files.write(path, data);

        System.out.println("File was written to " + path);

        return null;
    }

    private record DependencyManifest(@NotNull String baseUrl, @NotNull List<File> files, @NotNull List<Blob> blobs, @NotNull List<Pack> packs) {
        @NotNull
        public static DependencyManifest parse(@NotNull InputStream is) throws Exception {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            final SAXParser parser = factory.newSAXParser();
            final DependencyManifestHandler handler = new DependencyManifestHandler();

            parser.parse(is, handler);

            return new DependencyManifest(
                handler.baseUrl,
                handler.files,
                handler.blobs,
                handler.packs
            );
        }

        public record File(@NotNull String name, @NotNull String hash) {}

        public record Blob(@NotNull String hash, @NotNull String packHash, long size, int packOffset) {}

        public record Pack(@NotNull String hash, @NotNull String remotePath, long size, int compressedSize) {}
    }

    private static class DependencyManifestHandler extends DefaultHandler {
        private final List<DependencyManifest.File> files = new ArrayList<>();
        private final List<DependencyManifest.Blob> blobs = new ArrayList<>();
        private final List<DependencyManifest.Pack> packs = new ArrayList<>();
        private String baseUrl;

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) {
            switch (name) {
                case "DependencyManifest" -> baseUrl = attributes.getValue("BaseUrl");
                case "File" -> files.add(new DependencyManifest.File(
                    attributes.getValue("Name"),
                    attributes.getValue("Hash")
                ));
                case "Blob" -> blobs.add(new DependencyManifest.Blob(
                    attributes.getValue("Hash"),
                    attributes.getValue("PackHash"),
                    Long.parseLong(attributes.getValue("Size")),
                    Integer.parseInt(attributes.getValue("PackOffset"))
                ));
                case "Pack" -> packs.add(new DependencyManifest.Pack(
                    attributes.getValue("Hash"),
                    attributes.getValue("RemotePath"),
                    Long.parseLong(attributes.getValue("Size")),
                    Integer.parseInt(attributes.getValue("CompressedSize"))
                ));
            }
        }
    }
}
