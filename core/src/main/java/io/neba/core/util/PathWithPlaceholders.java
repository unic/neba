package io.neba.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


import static java.util.stream.Collectors.toList;

/**
 * Represents a path consisting of segments that potentially represents resolvable variables.
 *
 * @author Olaf Otto
 */
public class PathWithPlaceholders {
    private final List<Segment> segments;
    private final boolean hasVariables;

    /**
     * @param path must not be <code>null</code>.
     */
    public PathWithPlaceholders(String path) {
        this(segments(path));
    }

    private PathWithPlaceholders(List<Segment> segments) {
        this.segments = segments;
        this.hasVariables = this.segments.stream().anyMatch(s -> s.isVariable);
    }

    /**
     * @param resolver must not be <code>null</code>.
     * @return a path with resolved segments, never <code>null</code>.
     */
    public PathWithPlaceholders resolve(Function<String, String> resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("Method argument resolver must not be null");
        }

        if (!this.hasVariables) {
            return this;
        }

        return new PathWithPlaceholders(
                this.segments
                        .stream()
                        .map(s -> s.isVariable ?  new Segment(resolver.apply(s.value)) : s)
                        .collect(toList()));
    }

    @Override
    public String toString() {
        return this.segments.stream().map(s -> s.value).reduce("", String::concat);
    }

    public boolean hasVariables() {
        return this.hasVariables;
    }

    private static class Segment {
        private final boolean isVariable;
        private final String value;

        private Segment(boolean isVariable, String value) {
            this.isVariable = isVariable;
            this.value = value;
        }

        private Segment(String value) {
            this.isVariable = false;
            this.value = value;
        }
    }

    private static List<Segment> segments(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Method argument path must not be null");
        }

        final int length = path.length();
        final StringBuilder builder = new StringBuilder(length);
        List<Segment> segments = new ArrayList<>(4);

        for (int i = 0; i < length; ++i) {
            if (path.charAt(i) == '$' && i < length - 1) {
                ++i;
                if (path.charAt(i) == '{') {
                    int varStart = ++i;
                    for (; i < length; ++i) {
                        if (path.charAt(i) == '}') {
                            // We have read all non-variable segments and the variable segment.
                            // At this point, the buffer contains all non-variable characters.
                            if (builder.length() != 0) {
                                segments.add(new Segment(builder.toString()));
                                builder.setLength(0);
                            }
                            String varName = path.substring(varStart, i);
                            segments.add(new Segment(true, varName));
                            break;
                        }
                    }
                    if (i >= length) {
                        builder.append(path.substring(varStart - 2, i));
                    }
                } else {
                    builder.append(path.charAt(i - 1));
                    builder.append(path.charAt(i));
                }
            } else {
                builder.append(path.charAt(i));
            }
        }
        if (builder.length() != 0) {
            segments.add(new Segment(builder.toString()));
        }
        return segments;
    }
}
