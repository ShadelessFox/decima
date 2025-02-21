package com.shade.decima.app.ui.util;

import com.shade.util.NotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Listeners<T extends EventListener> {
    private final List<T> listeners = new CopyOnWriteArrayList<>();
    private final T broadcast;

    public Listeners(@NotNull Class<T> cls) {
        this.broadcast = cls.cast(Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{cls},
            new Broadcaster(this)
        ));
    }

    public void add(@NotNull T listener) {
        listeners.add(listener);
    }

    public void remove(@NotNull T listener) {
        listeners.remove(listener);
    }

    @NotNull
    public T broadcast() {
        return broadcast;
    }

    private record Broadcaster(Listeners<?> that) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "Proxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }

            publish(method, args);
            return null;
        }

        private void publish(Method method, Object[] args) throws Throwable {
            List<?> listeners = that.listeners;
            if (listeners.isEmpty()) {
                return;
            }
            for (Object listener : listeners) {
                method.invoke(listener, args);
            }
        }
    }
}
