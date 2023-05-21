package com.shade.platform.model.messages;

import com.shade.platform.model.Disposable;
import com.shade.util.NotNull;

public interface MessageBusConnection extends Disposable {
    <T> void subscribe(@NotNull Topic<T> topic, @NotNull T handler);
}
