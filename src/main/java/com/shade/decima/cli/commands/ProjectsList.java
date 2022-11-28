package com.shade.decima.cli.commands;

import com.shade.decima.cli.ApplicationCLI;
import com.shade.decima.model.app.ProjectContainer;
import picocli.CommandLine.Command;

import java.util.StringJoiner;

@Command(name = "list", description = "List all available projects")
public class ProjectsList implements Runnable {
    @Override
    public void run() {
        final StringJoiner buffer = new StringJoiner("\n");

        for (ProjectContainer container : ApplicationCLI.getWorkspace().getProjects()) {
            buffer.add(
                // @formatter:off
                "Id:                   " + container.getId() + '\n' +
                "Name:                 " + container.getName() + '\n' +
                "Type:                 " + container.getType() + '\n' +
                "ExecutablePath:       " + container.getExecutablePath() + '\n' +
                "CompressorPath:       " + container.getCompressorPath() + '\n' +
                "PackfilesPath:        " + container.getPackfilesPath() + '\n' +
                "TypeMetadataPath:     " + container.getTypeMetadataPath() + '\n' +
                "PackfileMetadataPath: " + container.getPackfileMetadataPath() + '\n' +
                "FileListingsPath:     " + container.getFileListingsPath() + '\n'
                // @formatter:on
            );
        }

        System.out.println(buffer);
    }
}
