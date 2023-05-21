package com.shade.platform.model.messages;

import com.shade.platform.model.Disposable;
import com.shade.platform.model.app.ApplicationManager;
import com.shade.util.NotNull;

public interface MessageBus extends Disposable {
    @NotNull
    static MessageBus getInstance() {
        return ApplicationManager.getApplication().getService(MessageBus.class);
    }

    @NotNull
    MessageBusConnection connect();

    @NotNull
    <T> T publisher(@NotNull Topic<T> topic);
}
