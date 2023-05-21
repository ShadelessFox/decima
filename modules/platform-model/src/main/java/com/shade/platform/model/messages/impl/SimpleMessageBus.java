package com.shade.platform.model.messages.impl;

import com.shade.platform.model.Service;
import com.shade.platform.model.messages.MessageBus;
import com.shade.platform.model.messages.MessageBusConnection;
import com.shade.platform.model.messages.Topic;
import com.shade.platform.model.util.ReflectionUtils;
import com.shade.util.NotNull;
import com.shade.util.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service(MessageBus.class)
public class SimpleMessageBus implements MessageBus {
    interface MessageHandlerHolder {
        void collectHandlers(@NotNull Topic<?> topic, @NotNull List<Object> result);

        boolean isDisposed();
    }

    private final Queue<MessageHandlerHolder> subscribers = new ConcurrentLinkedQueue<>();
    private final Map<Topic<?>, Object> publisherCache = new ConcurrentHashMap<>();
    private final Map<Topic<?>, Object[]> subscriberCache = new ConcurrentHashMap<>();
    private boolean disposed;

    @NotNull
    @Override
    public MessageBusConnection connect() {
        final SimpleMessageBusConnection connection = new SimpleMessageBusConnection(this);
        subscribers.offer(connection);
        return connection;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <T> T publisher(@NotNull Topic<T> topic) {
        if (disposed) {
            throw new IllegalStateException("Already disposed: " + this);
        }

        return (T) publisherCache.computeIfAbsent(topic, topic1 -> {
            final var type = topic1.type();
            final var publisher = createPublisher(topic1);
            return Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, publisher);
        });
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }

        subscribers.clear();
        publisherCache.clear();
        subscriberCache.clear();
        disposed = true;
    }

    public void notifySubscriptionAdded(@NotNull Topic<?> topic) {
        subscriberCache.remove(topic);
    }

    public void notifyConnectionTerminated(@NotNull Set<Map.Entry<Topic<?>, Object>> handlers) {
        for (Map.Entry<Topic<?>, Object> handler : handlers) {
            subscriberCache.remove(handler.getKey());
        }

        subscribers.removeIf(MessageHandlerHolder::isDisposed);
    }

    @NotNull
    private Object[] computeSubscribers(@NotNull Topic<?> topic) {
        final List<Object> result = new ArrayList<>();

        for (MessageHandlerHolder subscriber : subscribers) {
            if (!subscriber.isDisposed()) {
                subscriber.collectHandlers(topic, result);
            }
        }

        return result.toArray(Object[]::new);
    }

    @NotNull
    private InvocationHandler createPublisher(@NotNull Topic<?> topic) {
        return new MessagePublisher<>(topic, this);
    }

    private static record MessagePublisher<T>(@NotNull Topic<T> topic, @NotNull SimpleMessageBus bus) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return ReflectionUtils.handleObjectMethod(proxy, method, args);
            }

            publish(method, args);
            return null;
        }

        private void publish(@NotNull Method method, @Nullable Object[] args) {
            final Object[] handlers = bus.subscriberCache.computeIfAbsent(topic, bus::computeSubscribers);

            if (handlers.length == 0) {
                return;
            }

            RuntimeException error = null;

            for (Object handler : handlers) {
                error = invokeListener(method, args, handler, error);
            }

            if (error != null) {
                throw error;
            }
        }

        @Nullable
        private RuntimeException invokeListener(
            @NotNull Method method,
            @Nullable Object[] args,
            @NotNull Object handler,
            @Nullable RuntimeException lastError
        ) {
            try {
                method.invoke(handler, args);
            } catch (Throwable e) {
                final RuntimeException error = new RuntimeException(
                    "Cannot invoke (" +
                        "class=" + handler.getClass() +
                        ", method=" + method.getName() +
                        ", topic=" + topic.name() + ")", e);

                if (lastError == null) {
                    lastError = error;
                } else {
                    lastError.addSuppressed(error);
                }
            }

            return lastError;
        }
    }
}
