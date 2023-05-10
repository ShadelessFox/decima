package com.shade.decima.cli.commands;

import picocli.CommandLine.Command;

@Command(name = "projects", description = "Project management", subcommands = ProjectsList.class)
public class Projects {
}
