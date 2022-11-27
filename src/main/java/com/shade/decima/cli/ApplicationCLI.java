package com.shade.decima.cli;

import com.shade.decima.cli.commands.DumpFilePaths;
import com.shade.decima.cli.converters.ProjectConverter;
import com.shade.decima.model.app.Project;
import com.shade.decima.model.app.Workspace;
import com.shade.util.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(subcommands = {
    DumpFilePaths.class
})
public class ApplicationCLI {
    private static final Workspace workspace = new Workspace();

    public static void main(String[] args) {
        final int status = new CommandLine(ApplicationCLI.class)
            .registerConverter(Project.class, new ProjectConverter())
            .execute(args);

        System.exit(status);
    }

    @NotNull
    public static Workspace getWorkspace() {
        return workspace;
    }
}
