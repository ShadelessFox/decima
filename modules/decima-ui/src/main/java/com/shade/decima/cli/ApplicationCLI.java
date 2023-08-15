package com.shade.decima.cli;

import com.shade.decima.cli.commands.DumpEntryPointNames;
import com.shade.decima.cli.commands.DumpFilePaths;
import com.shade.decima.cli.commands.Projects;
import com.shade.decima.cli.commands.RepackArchive;
import com.shade.decima.cli.converters.ProjectConverter;
import com.shade.decima.model.app.Project;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(subcommands = {
    DumpFilePaths.class,
    DumpEntryPointNames.class,
    Projects.class,
    RepackArchive.class
})
public class ApplicationCLI {
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message")
    private boolean usageHelpRequested;

    public static void execute(String[] args) {
        final int status = new CommandLine(ApplicationCLI.class)
            .registerConverter(Project.class, new ProjectConverter())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(args);

        System.exit(status);
    }
}
