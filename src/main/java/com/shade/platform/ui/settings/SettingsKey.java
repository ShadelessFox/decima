package com.shade.platform.ui.settings;

import com.shade.util.NotNull;

import java.util.prefs.Preferences;

public final class SettingsKey<T> {
    private final String key;
    private final Getter<T> getter;
    private final Setter<T> setter;
    private final T defaultValue;

    private SettingsKey(@NotNull String key, @NotNull Getter<T> getter, @NotNull Setter<T> setter, @NotNull T defaultValue) {
        this.key = key;
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
    }

    @NotNull
    public static SettingsKey<String> of(@NotNull String key, @NotNull String def) {
        return new SettingsKey<>(key, Preferences::get, Preferences::put, def);
    }

    @NotNull
    public static SettingsKey<Boolean> of(@NotNull String key, boolean def) {
        return new SettingsKey<>(key, Preferences::getBoolean, Preferences::putBoolean, def);
    }

    @NotNull
    public static SettingsKey<Integer> of(@NotNull String key, int def) {
        return new SettingsKey<>(key, Preferences::getInt, Preferences::putInt, def);
    }

    @NotNull
    public static SettingsKey<Long> of(@NotNull String key, long def) {
        return new SettingsKey<>(key, Preferences::getLong, Preferences::putLong, def);
    }

    @NotNull
    public static SettingsKey<Float> of(@NotNull String key, float def) {
        return new SettingsKey<>(key, Preferences::getFloat, Preferences::putFloat, def);

    }

    @NotNull
    public static SettingsKey<Double> of(@NotNull String key, double def) {
        return new SettingsKey<>(key, Preferences::getDouble, Preferences::putDouble, def);
    }

    @NotNull
    public String key() {
        return key;
    }

    @NotNull
    public T get(@NotNull Preferences preferences) {
        return getter.get(preferences, key, defaultValue);
    }

    public void set(@NotNull Preferences preferences, @NotNull T value) {
        if (defaultValue.equals(value)) {
            clear(preferences);
        } else {
            setter.set(preferences, key, value);
        }
    }

    public void clear(@NotNull Preferences preferences) {
        preferences.remove(key);
    }

    interface Getter<T> {
        T get(@NotNull Preferences preferences, @NotNull String key, @NotNull T def);
    }

    interface Setter<T> {
        void set(@NotNull Preferences preferences, @NotNull String key, @NotNull T value);
    }
}
