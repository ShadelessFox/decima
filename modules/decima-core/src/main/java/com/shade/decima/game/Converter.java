package com.shade.decima.game;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

public interface Converter<T extends Game, R> {
    @SuppressWarnings("unchecked")
    static <T extends Game> Stream<Converter<T, ?>> converters() {
        return ServiceLoader.load(Converter.class).stream().map(x -> (Converter<T, ?>) x.get());
    }

    static <T extends Game, R> Optional<R> convert(Object object, T game, Class<R> clazz) {
        return Converter.<T>converters()
            .filter(c -> clazz.isAssignableFrom(c.resultType()) && c.supports(object))
            .findFirst()
            .flatMap(c -> c.convert(object, game).map(clazz::cast));
    }

    boolean supports(Object object);

    Optional<R> convert(Object object, T game);

    @SuppressWarnings("unchecked")
    default Class<R> resultType() {
        return Stream.of(getClass().getGenericInterfaces())
            .map(ParameterizedType.class::cast)
            .filter(type -> type.getRawType() == Converter.class)
            .map(type -> (Class<R>) type.getActualTypeArguments()[1])
            .findFirst().orElseThrow();
    }
}
