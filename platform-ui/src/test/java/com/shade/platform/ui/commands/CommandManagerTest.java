package com.shade.platform.ui.commands;

import com.shade.util.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CommandManagerTest {
    private CommandManager manager;

    @BeforeEach
    public void setup() {
        manager = new CommandManager();
    }

    @Test
    @DisplayName("Must not redo or undo if there's no commands")
    public void emptyCommandManagerTest() {
        assertFalse(manager.canRedo());
        assertThrows(IllegalStateException.class, manager::redo);

        assertFalse(manager.canUndo());
        assertThrows(IllegalStateException.class, manager::undo);
    }

    @Test
    @DisplayName("Must undo and redo a single command")
    public void nonEmptyCommandManagerTest() {
        manager.add(new DummyCommand("A"));

        assertFalse(manager.canRedo());
        assertTrue(manager.canUndo());
        assertEquals("A", manager.getUndoTitle());

        manager.undo();

        assertTrue(manager.canRedo());
        assertFalse(manager.canUndo());

        manager.redo();

        assertFalse(manager.canRedo());
        assertTrue(manager.canUndo());
        assertEquals("A", manager.getUndoTitle());
    }

    @Test
    @DisplayName("Must overwrite undoed commands after undo() and add()")
    public void commandOverwriteCommandManagerTest() {
        manager.add(new DummyCommand("A"));
        manager.add(new DummyCommand("B"));

        assertEquals("B", manager.getUndoTitle());

        manager.undo();
        assertEquals("A", manager.getUndoTitle());

        manager.add(new DummyCommand("C"));
        assertEquals("C", manager.getUndoTitle());
    }

    private static class DummyCommand extends BaseCommand {
        private final String name;

        public DummyCommand(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        @Override
        protected String getTitle() {
            return name;
        }
    }
}
