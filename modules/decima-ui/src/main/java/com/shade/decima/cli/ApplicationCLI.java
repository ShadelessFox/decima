package com.shade.decima.cli;

import com.shade.decima.cli.commands.*;
import com.shade.decima.cli.converters.ProjectConverter;
import com.shade.decima.model.app.Project;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(subcommands = {
    DumpFilePaths.class,
    DumpEntryPointNames.class,
    DumpFileReferences.class,
    Projects.class,
    RepackArchive.class,
    GetOodleLibrary.class
})
public class ApplicationCLI {
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    private boolean usageHelpRequested;

    public static void execute(String[] args) {
        final int status = new CommandLine(ApplicationCLI.class)
            .registerConverter(Project.class, new ProjectConverter())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setUsageHelpWidth(200)
            .execute(args);

        System.exit(status);
    }
}
