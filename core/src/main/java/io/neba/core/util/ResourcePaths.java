package io.neba.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;

/**
 * A factory for {@link ResourcePath paths}.
 */
public class ResourcePaths {
    private static final String MATCH_NAME = "placeholder";
    private static final Pattern PLACEHOLDER = compile("\\$\\{(?<" + MATCH_NAME + ">[^}|\\s]+)}");

    /**
     * @param path must not be <code>null</code>.
     * @return never <code>null</code>.
     */
    public static ResourcePath path(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Method argument path must not be null");
        }

        if (PLACEHOLDER.matcher(path).find()) {
            return new PathWithPlaceholders(path);
        }
        return new PathWithoutPlaceholders(path);
    }

    /**
     * Represents a path consisting of segments that may represent placeholders of the form
     * <code>${name}</code> which can be resolved via {@link ResourcePath#resolve(Function)}.
     *
     * @author Olaf Otto
     */
    public interface ResourcePath {
        default boolean hasPlaceholders() {
            return false;
        }

        /**
         * @param resolver must not be <code>null</code>.
         * @return a path with resolved segments, never <code>null</code>.
         */
        ResourcePath resolve(Function<String, String> resolver);
    }

    private static class PathWithPlaceholders implements ResourcePath {
        private final List<Value> segments;

        private PathWithPlaceholders(String path) {
            this(segments(path));
        }

        private PathWithPlaceholders(List<Value> segments) {
            this.segments = segments;
        }

        public ResourcePath resolve(Function<String, String> resolver) {
            if (resolver == null) {
                throw new IllegalArgumentException("Method argument resolver must not be null");
            }

            return new PathWithPlaceholders(
                    this.segments
                            .stream()
                            .map(s -> s.isPlaceholder() ? new Value(resolver.apply(s.value)) : s)
                            .collect(toList()));
        }

        @Override
        public String toString() {
            return this.segments.stream().map(s -> s.value).reduce("", String::concat);
        }

        public boolean hasPlaceholders() {
            return true;
        }

        private static class Value {
            private final String value;

            private Value(String value) {
                this.value = value;
            }

            public String getValue() {
                return this.value;
            }

            public boolean isPlaceholder() {
                return false;
            }
        }

        private static class Placeholder extends Value {
            private Placeholder(String value) {
                super(value);
            }

            @Override
            public boolean isPlaceholder() {
                return true;
            }
        }

        private static List<Value> segments(String path) {
            if (path == null) {
                throw new IllegalArgumentException("Method argument path must not be null");
            }

            final Matcher matcher = PLACEHOLDER.matcher(path);
            if (!matcher.find()) {
                return Collections.singletonList(new Value(path));
            }
            List<Value> segments = new ArrayList<>(4);
            int last = 0;
            do {
                if (matcher.start() != 0) {
                    segments.add(new Value(path.substring(last, matcher.start())));
                }
                segments.add(new Placeholder(matcher.group(MATCH_NAME)));
                last = matcher.end();
            } while (matcher.find());

            if (last != path.length()) {
                segments.add(new Value(path.substring(last)));
            }
            return segments;

        }
    }

    private static class PathWithoutPlaceholders implements ResourcePath {
        private final String path;

        private PathWithoutPlaceholders(String path) {
            this.path = path;
        }

        @Override
        public ResourcePath resolve(Function<String, String> resolver) {
            return this;
        }

        @Override
        public String toString() {
            return this.path;
        }
    }
}
