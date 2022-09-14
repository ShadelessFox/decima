package com.shade.platform.ui.commands;

import com.shade.util.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class CommandManager implements Command {
    private final List<CommandManagerChangeListener> listeners = new ArrayList<>();
    private final List<Command> commands = new ArrayList<>();
    private int cursor;

    public void add(@NotNull Command command) {
        clearUndoCommands();

        commands.add(command);

        redo();
    }

    public void discardAllCommands() {
        commands.clear();
        cursor = 0;
    }

    public void clearUndoCommands() {
        if (commands.size() > cursor) {
            commands.subList(cursor, commands.size()).clear();
        }
    }

    @Override
    public void redo() {
        if (!canRedo()) {
            throw new IllegalStateException("Can't redo");
        }

        final Command command = commands.get(cursor++);

        command.redo();
        fireChangeEvent(CommandManagerChangeListener::commandDidRedo, command);
    }

    @Override
    public void undo() {
        if (!canUndo()) {
            throw new IllegalStateException("Can't undo");
        }

        final Command command = commands.get(--cursor);

        command.undo();
        fireChangeEvent(CommandManagerChangeListener::commandDidUndo, command);
    }

    @Override
    public boolean canRedo() {
        return cursor < commands.size();
    }

    @Override
    public boolean canUndo() {
        return cursor > 0;
    }

    @NotNull
    @Override
    public String getUndoTitle() {
        if (commands.isEmpty()) {
            throw new IllegalStateException("No undo commands");
        } else {
            return commands.get(cursor - 1).getUndoTitle();
        }
    }

    @NotNull
    @Override
    public String getRedoTitle() {
        if (commands.isEmpty()) {
            throw new IllegalStateException("No redo commands");
        } else {
            return commands.get(cursor).getUndoTitle();
        }
    }

    public void addChangeListener(@NotNull CommandManagerChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(@NotNull CommandManagerChangeListener listener) {
        listeners.remove(listener);
    }

    private void fireChangeEvent(@NotNull BiConsumer<CommandManagerChangeListener, Command> consumer, @NotNull Command command) {
        for (CommandManagerChangeListener listener : listeners) {
            consumer.accept(listener, command);
        }
    }
}
