package com.shade.platform.model.messages.impl;

import com.shade.platform.model.messages.MessageBusConnection;
import com.shade.platform.model.messages.Topic;
import com.shade.util.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleMessageBusConnection implements MessageBusConnection, SimpleMessageBus.MessageHandlerHolder {
    private SimpleMessageBus bus;
    private final Map<Topic<?>, Object> handlers;

    public SimpleMessageBusConnection(@NotNull SimpleMessageBus bus) {
        this.bus = bus;
        this.handlers = new ConcurrentHashMap<>();
    }

    @Override
    public <T> void subscribe(@NotNull Topic<T> topic, @NotNull T handler) {
        if (bus == null) {
            throw new IllegalStateException("Already disposed: " + this);
        }

        if (handlers.putIfAbsent(topic, handler) != null) {
            throw new IllegalStateException("A handler for topic '" + topic.name() + "' is already registered within this connection");
        }

        bus.notifySubscriptionAdded(topic);
    }

    @Override
    public void collectHandlers(@NotNull Topic<?> topic, @NotNull List<Object> result) {
        final Object handler = handlers.get(topic);
        if (handler != null) {
            result.add(handler);
        }
    }

    @Override
    public void dispose() {
        if (bus == null) {
            return;
        }

        final SimpleMessageBus bus = this.bus;
        this.bus = null;
        bus.notifyConnectionTerminated(handlers.entrySet());
        handlers.clear();
    }

    @Override
    public boolean isDisposed() {
        return bus == null;
    }
}
