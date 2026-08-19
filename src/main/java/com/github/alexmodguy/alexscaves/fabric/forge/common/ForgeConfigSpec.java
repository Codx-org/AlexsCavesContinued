package com.github.alexmodguy.alexscaves.fabric.forge.common;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fabric stand-in for Forge's declarative config spec.
 *
 * <p><b>Why a stand-in rather than a migration.</b> This mod declares 58 options across
 * {@code ACServerConfig} and {@code ACClientConfig} and reads them back at 106 call sites, all of
 * the shape {@code AlexsCaves.COMMON_CONFIG.someOption.get()}. Moving to a different config library
 * would rewrite every one of those lines on all 58 nodes to buy something only the 22 Fabric ones
 * need. Reproducing the builder instead leaves both spec classes and all 106 reads byte-identical
 * on every loader, and confines the whole difference to this file plus one gated arm in
 * {@code AlexsCaves}'s constructor.
 *
 * <p><b>It writes the same file, in the same format, at the same path</b> —
 * {@code config/alexscaves-general.toml} and {@code config/alexscaves-client.toml}, TOML with the
 * comments and {@code #Range:} lines Forge emits. That is deliberate: a pack that moves between
 * loaders keeps its tuning, and a player following a Forge-era guide finds the file where the guide
 * says it is. The subset of TOML involved is small — sections, scalars, {@code #} comments, no
 * arrays, no inline tables, no multi-line strings — because that is all the builder below can
 * produce, so a hand-rolled reader is honest rather than a shortcut.
 *
 * <p><b>The API surface is deliberately closed</b>, exactly as wide as the two spec classes:
 * {@link Builder#comment}, {@link Builder#translation}, {@link Builder#push}, {@link Builder#pop},
 * {@link Builder#define(String, boolean)}, the two {@code defineInRange} overloads,
 * {@link Builder#configure}, and {@link ConfigValue#get()}. If a new option needs a shape that is
 * not here, widen this class — never the Stonecutter rule that points at it.
 *
 * <p><b>What is not reproduced:</b> there is no config-changed event and no runtime reload. On the
 * other two loaders the loader owns the file and re-fires its config events when it is edited;
 * here {@link #load(Path)} runs once, from the constructor, before anything reads a value. An edit
 * takes effect on the next launch, which is what a {@code worldRestart} option does on every
 * loader anyway — and none of this mod's 58 options is watched for live changes on Forge either.
 *
 * <p>An unreadable or malformed file is never fatal: the offending line is logged and its option
 * keeps its default, and the file is rewritten in canonical form afterwards, so a corrupted config
 * repairs itself rather than stopping the game.
 */
public final class ForgeConfigSpec {

    private final List<ConfigValue<?>> values;

    private ForgeConfigSpec(List<ConfigValue<?>> values) {
        this.values = values;
    }

    /**
     * Reads {@code file} into this spec's values, creating it from the defaults if it is absent,
     * and writes it back in canonical form.
     *
     * <p>The write-back is what adds options introduced by a mod update to an existing file, and
     * what restores a comment somebody deleted. It happens on every launch; the content is a pure
     * function of the values, so it is a no-op on disk unless something actually changed.
     */
    public void load(Path file) {
        Map<String, String> raw = new HashMap<>();
        if (Files.exists(file)) {
            try {
                parseInto(Files.readAllLines(file, StandardCharsets.UTF_8), raw, file);
            } catch (IOException e) {
                AlexsCaves.LOGGER.error("Could not read " + file + " — every option keeps its default", e);
            }
        }
        for (ConfigValue<?> value : values) {
            String found = raw.get(value.path);
            if (found != null) {
                value.parse(found, file);
            }
        }
        write(file);
    }

    /**
     * Fills {@code into} with {@code section.key -> raw value text}. Deliberately forgiving: a line
     * it cannot make sense of is reported and skipped rather than aborting the read, so one bad
     * edit costs one option rather than all of them.
     */
    private static void parseInto(List<String> lines, Map<String, String> into, Path file) {
        String section = "";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.substring(1, trimmed.length() - 1).trim();
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (equals < 0) {
                AlexsCaves.LOGGER.warn("Ignoring unreadable line in " + file + ": " + trimmed);
                continue;
            }
            String key = trimmed.substring(0, equals).trim();
            String value = trimmed.substring(equals + 1).trim();
            into.put(section.isEmpty() ? key : section + "." + key, value);
        }
    }

    /**
     * Emits the whole spec in declaration order. Because the two spec classes declare their options
     * grouped between {@code push}/{@code pop} pairs, walking the values in order and opening a
     * section whenever the prefix changes reproduces the layout Forge writes, headers included.
     */
    private void write(Path file) {
        StringBuilder out = new StringBuilder();
        String section = null;
        for (ConfigValue<?> value : values) {
            if (!value.section.equals(section)) {
                section = value.section;
                if (out.length() > 0) {
                    out.append('\n');
                }
                if (!section.isEmpty()) {
                    out.append('[').append(section).append(']').append('\n');
                }
            }
            String indent = section.isEmpty() ? "" : "\t";
            if (value.comment != null) {
                out.append(indent).append('#').append(value.comment).append('\n');
            }
            String range = value.rangeComment();
            if (range != null) {
                out.append(indent).append('#').append(range).append('\n');
            }
            out.append(indent).append(value.key).append(" = ").append(value.write()).append('\n');
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, out.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            AlexsCaves.LOGGER.error("Could not write " + file + " — this session's values are still correct, "
                    + "but any edit to them will not survive a restart", e);
        }
    }

    public static final class Builder {

        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<String> stack = new ArrayList<>();
        private String pendingComment;

        /**
         * ⚠️ Overwrites the pending comment rather than appending to it, exactly as Forge's does.
         * Two {@code comment(...)} calls before one {@code define} therefore ship only the second
         * line — the same trap the sibling mods hit with CodxLib's builder. Neither spec class in
         * this tree does that today; keep it that way.
         */
        public Builder comment(String comment) {
            pendingComment = comment;
            return this;
        }

        /**
         * Accepted and discarded. It names a translation key for Forge's generated config screen,
         * which no loader in this matrix renders any more (Forge dropped the built-in screen years
         * ago and the two spec classes' keys have never had translations shipped for them). Kept in
         * the signature so the 54 call sites need no gate.
         */
        public Builder translation(String key) {
            return this;
        }

        public Builder push(String path) {
            stack.add(path);
            return this;
        }

        public Builder pop() {
            if (!stack.isEmpty()) {
                stack.remove(stack.size() - 1);
            }
            return this;
        }

        public BooleanValue define(String path, boolean defaultValue) {
            return add(new BooleanValue(section(), path, take(), defaultValue));
        }

        public IntValue defineInRange(String path, int defaultValue, int min, int max) {
            return add(new IntValue(section(), path, take(), defaultValue, min, max));
        }

        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            return add(new DoubleValue(section(), path, take(), defaultValue, min, max));
        }

        /**
         * Runs {@code factory} against this builder and hands back both halves, matching the shape
         * {@code AlexsCaves}'s static initialiser destructures.
         */
        public <T> Pair<T, ForgeConfigSpec> configure(Function<Builder, T> factory) {
            T configured = factory.apply(this);
            return Pair.of(configured, new ForgeConfigSpec(values));
        }

        private <V extends ConfigValue<?>> V add(V value) {
            values.add(value);
            return value;
        }

        private String section() {
            return String.join(".", stack);
        }

        private String take() {
            String comment = pendingComment;
            pendingComment = null;
            return comment;
        }
    }

    /**
     * One option. Implements {@link Supplier} because that is what Forge's does and what a handful
     * of call sites lean on when they pass an option straight into something expecting a supplier.
     */
    public abstract static class ConfigValue<T> implements Supplier<T> {

        final String section;
        final String key;
        final String path;
        final String comment;
        T value;

        ConfigValue(String section, String key, String comment, T defaultValue) {
            this.section = section;
            this.key = key;
            this.path = section.isEmpty() ? key : section + "." + key;
            this.comment = comment;
            this.value = defaultValue;
        }

        @Override
        public T get() {
            return value;
        }

        abstract void parse(String raw, Path file);

        abstract String write();

        String rangeComment() {
            return null;
        }

        void reject(String raw, Path file) {
            AlexsCaves.LOGGER.warn("Ignoring out-of-range or unreadable value for " + path + " in " + file
                    + " (" + raw + ") — using the default " + value);
        }
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {

        BooleanValue(String section, String key, String comment, boolean defaultValue) {
            super(section, key, comment, defaultValue);
        }

        @Override
        void parse(String raw, Path file) {
            if ("true".equalsIgnoreCase(raw)) {
                value = Boolean.TRUE;
            } else if ("false".equalsIgnoreCase(raw)) {
                value = Boolean.FALSE;
            } else {
                reject(raw, file);
            }
        }

        @Override
        String write() {
            return value.toString();
        }
    }

    public static final class IntValue extends ConfigValue<Integer> {

        private final int min;
        private final int max;

        IntValue(String section, String key, String comment, int defaultValue, int min, int max) {
            super(section, key, comment, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        void parse(String raw, Path file) {
            try {
                int parsed = Integer.parseInt(raw);
                if (parsed < min || parsed > max) {
                    reject(raw, file);
                    return;
                }
                value = parsed;
            } catch (NumberFormatException e) {
                reject(raw, file);
            }
        }

        @Override
        String write() {
            return value.toString();
        }

        @Override
        String rangeComment() {
            return "Range: " + min + " ~ " + max;
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> {

        private final double min;
        private final double max;

        DoubleValue(String section, String key, String comment, double defaultValue, double min, double max) {
            super(section, key, comment, defaultValue);
            this.min = min;
            this.max = max;
        }

        @Override
        void parse(String raw, Path file) {
            try {
                double parsed = Double.parseDouble(raw);
                if (!(parsed >= min) || !(parsed <= max)) {
                    reject(raw, file);
                    return;
                }
                value = parsed;
            } catch (NumberFormatException e) {
                reject(raw, file);
            }
        }

        /**
         * Always carries a decimal point, so the value re-reads as a float rather than as an
         * integer — the one place where a round-trip through this file could otherwise change a
         * value's type.
         */
        @Override
        String write() {
            String written = value.toString();
            return written.indexOf('.') < 0 && written.indexOf('E') < 0 ? written + ".0" : written;
        }

        @Override
        String rangeComment() {
            return "Range: " + min + " ~ " + max;
        }
    }
}
