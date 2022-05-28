package com.shade.decima.ui.action;

import com.shade.decima.model.util.NotNull;
import com.shade.decima.ui.UIUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;

public final class Actions {
    private static final Logger log = LoggerFactory.getLogger(Actions.class);
    private static final Map<String, ActionInfo> actions = new HashMap<>();
    private static final Map<String, List<ActionInfo>> contributions = new TreeMap<>();

    static {
        final Reflections reflections = new Reflections("com.shade.decima.ui");
        final Set<Class<?>> types = reflections.getTypesAnnotatedWith(ActionRegistration.class);

        for (Class<?> type : types) {
            final ActionInfo info = ActionInfo.create(type);

            // FIXME: This code supposed to do something
            // if (actions.containsKey(info.registration().id())) {
            //     log.warn("Duplicate action '" + info.registration().id() + "'");
            //     continue;
            // }

            contributions
                .computeIfAbsent(info.contribution().path(), path -> new ArrayList<>())
                .add(info);
        }

        for (List<ActionInfo> actions : contributions.values()) {
            actions.sort(Comparator.comparingInt(info -> info.contribution().position()));
        }
    }

    private Actions() {
    }

    public static void contribute(@NotNull JComponent container, @NotNull String path) {
        final List<ActionInfo> actions = contributions.get(path);

        if (actions == null || actions.isEmpty()) {
            return;
        }

        for (ActionInfo action : actions) {
            if ((action.contribution().separator() & ActionContribution.SEPARATOR_BEFORE) > 0) {
                container.add(new JSeparator());
            }

            container.add(new JMenuItem(action.action()));

            if ((action.contribution().separator() & ActionContribution.SEPARATOR_AFTER) > 0) {
                container.add(new JSeparator());
            }
        }
    }

    private static record ActionInfo(@NotNull Action action, @NotNull ActionRegistration registration, @NotNull ActionContribution contribution) {
        public static ActionInfo create(@NotNull Class<?> type) {
            if (!Action.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Class " + type + " does not inherit from " + Action.class);
            }

            final ActionRegistration registration = type.getAnnotation(ActionRegistration.class);
            final ActionContribution contribution = type.getAnnotation(ActionContribution.class);
            final Action action;

            try {
                action = (Action) type.getConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Can't construct action", e);
            }

            final UIUtils.Mnemonic mnemonic = UIUtils.extractMnemonic(registration.name());

            if (mnemonic == null) {
                action.putValue(Action.NAME, registration.name());
            } else {
                action.putValue(Action.NAME, mnemonic.text());
                action.putValue(Action.MNEMONIC_KEY, mnemonic.key());
                action.putValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY, mnemonic.index());
            }

            if (!registration.description().isEmpty()) {
                action.putValue(Action.SHORT_DESCRIPTION, registration.description());
            }

            if (!registration.accelerator().isEmpty()) {
                action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(registration.accelerator()));
            }

            return new ActionInfo(action, registration, contribution);
        }
    }
}
