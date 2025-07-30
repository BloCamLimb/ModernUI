/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.util;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Framework wrapper over SLF4J API for sending log output.
 * <p>
 * Since 3.12.0, the ModernUI framework uses SLF4J for logging, applications should
 * use their own logging API implementation.
 */
public final class Log {

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final Logger LOGGER = LoggerFactory.getLogger(ModernUI.NAME_CPT);

    /**
     * @hidden
     */
    @MagicConstant(intValues = {ASSERT, ERROR, WARN, INFO, DEBUG, VERBOSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Level {
    }

    /**
     * A fine-grained debug message, typically capturing the flow through the application.
     */
    public static final int VERBOSE = 2;

    /**
     * A general debugging event.
     */
    public static final int DEBUG = 3;

    /**
     * An event for informational purposes.
     */
    public static final int INFO = 4;

    /**
     * An event that might possible lead to an error.
     */
    public static final int WARN = 5;

    /**
     * An error in the application, possibly recoverable.
     */
    public static final int ERROR = 6;

    /**
     * @hidden
     */
    public static final int ASSERT = 7;

    /**
     * Exception class used to capture a stack trace in {@link #wtf}.
     *
     * @hidden
     */
    public static class TerribleFailure extends Exception {
        TerribleFailure(@Nullable String msg, @Nullable Throwable cause) {
            super(msg, cause);
        }
    }

    /**
     * Interface to handle terrible failures from {@link #wtf}.
     *
     * @hidden
     */
    @FunctionalInterface
    public interface TerribleFailureHandler {
        void onTerribleFailure(@Nullable Marker marker, @NonNull TerribleFailure what);
    }

    private static final AtomicReference<TerribleFailureHandler> sWtfHandler = new AtomicReference<>();

    private Log() {
    }

    /**
     * Logs a message object with the {@link #VERBOSE} level.
     *
     * @param marker  used to identify the source of a log message.
     * @param message the message string to log.
     */
    public static void v(@Nullable Marker marker, @Nullable Object message) {
        if (marker == null) {
            LOGGER.trace(String.valueOf(message));
        } else {
            LOGGER.trace(marker, String.valueOf(message));
        }
    }

    /**
     * Logs a message at the {@link #VERBOSE} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker    used to identify the source of a log message.
     * @param message   the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void v(@Nullable Marker marker, @Nullable Object message, Throwable throwable) {
        if (marker == null) {
            LOGGER.trace(String.valueOf(message), throwable);
        } else {
            LOGGER.trace(marker, String.valueOf(message), throwable);
        }
    }

    /**
     * Logs a message object with the {@link #DEBUG} level.
     *
     * @param marker  used to identify the source of a log message.
     * @param message the message string to log.
     */
    public static void d(@Nullable Marker marker, @Nullable Object message) {
        if (marker == null) {
            LOGGER.debug(String.valueOf(message));
        } else {
            LOGGER.debug(marker, String.valueOf(message));
        }
    }

    /**
     * Logs a message at the {@link #DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker    used to identify the source of a log message.
     * @param message   the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void d(@Nullable Marker marker, @Nullable Object message, Throwable throwable) {
        if (marker == null) {
            LOGGER.debug(String.valueOf(message), throwable);
        } else {
            LOGGER.debug(marker, String.valueOf(message), throwable);
        }
    }

    /**
     * Logs a message object with the {@link #INFO} level.
     *
     * @param marker  used to identify the source of a log message.
     * @param message the message string to log.
     */
    public static void i(@Nullable Marker marker, @Nullable Object message) {
        if (marker == null) {
            LOGGER.info(String.valueOf(message));
        } else {
            LOGGER.info(marker, String.valueOf(message));
        }
    }

    /**
     * Logs a message at the {@link #INFO} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker    used to identify the source of a log message.
     * @param message   the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void i(@Nullable Marker marker, @Nullable Object message, Throwable throwable) {
        if (marker == null) {
            LOGGER.info(String.valueOf(message), throwable);
        } else {
            LOGGER.info(marker, String.valueOf(message), throwable);
        }
    }

    /**
     * Logs a message object with the {@link #WARN} level.
     *
     * @param marker  used to identify the source of a log message.
     * @param message the message string to log.
     */
    public static void w(@Nullable Marker marker, @Nullable Object message) {
        if (marker == null) {
            LOGGER.warn(String.valueOf(message));
        } else {
            LOGGER.warn(marker, String.valueOf(message));
        }
    }

    /**
     * Logs a message at the {@link #WARN} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker    used to identify the source of a log message.
     * @param message   the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void w(@Nullable Marker marker, @Nullable Object message, Throwable throwable) {
        if (marker == null) {
            LOGGER.warn(String.valueOf(message), throwable);
        } else {
            LOGGER.warn(marker, String.valueOf(message), throwable);
        }
    }

    /**
     * Checks to see whether a log for the specified tag is enabled at the specified level.
     */
    public static boolean isLoggable(@Nullable Marker marker, @Level int level) {
        if (marker == null) {
            return LOGGER.isEnabledForLevel(toLogLevel(level));
        } else {
            return switch (level) {
                case VERBOSE -> LOGGER.isTraceEnabled(marker);
                case DEBUG -> LOGGER.isDebugEnabled(marker);
                case INFO -> LOGGER.isInfoEnabled(marker);
                case WARN -> LOGGER.isWarnEnabled(marker);
                case ERROR -> LOGGER.isErrorEnabled(marker);
                default -> false;
            };
        }
    }

    /**
     * Logs a message object with the {@link #ERROR} level.
     *
     * @param marker  used to identify the source of a log message.
     * @param message the message string to log.
     */
    public static void e(@Nullable Marker marker, @Nullable Object message) {
        if (marker == null) {
            LOGGER.error(String.valueOf(message));
        } else {
            LOGGER.error(marker, String.valueOf(message));
        }
    }

    /**
     * Logs a message at the {@link #ERROR} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param marker    used to identify the source of a log message.
     * @param message   the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void e(@Nullable Marker marker, @Nullable Object message, Throwable throwable) {
        if (marker == null) {
            LOGGER.error(String.valueOf(message), throwable);
        } else {
            LOGGER.error(marker, String.valueOf(message), throwable);
        }
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     *
     * @param marker    The marker specific to the statement.
     * @param message   The message you would like logged.
     * @param throwable An exception to log.  May be null.
     */
    public static void wtf(@Nullable Marker marker, @Nullable String message, @Nullable Throwable throwable) {
        if (throwable == null) {
            e(marker, message);
        } else {
            e(marker, message, throwable);
        }
        TerribleFailureHandler handler = sWtfHandler.get();
        if (handler != null) {
            TerribleFailure what = new TerribleFailure(message, throwable);
            handler.onTerribleFailure(marker, what);
        }
    }

    /**
     * Sets the terrible failure handler.
     *
     * @return the old handler
     * @hidden
     */
    @Nullable
    public static TerribleFailureHandler setWtfHandler(@Nullable TerribleFailureHandler handler) {
        return sWtfHandler.getAndSet(handler);
    }

    /**
     * Logs a message.
     *
     * @param level   The logging level.
     * @param marker  Used to identify the source of a log message.
     * @param message The message String.
     */
    public static void println(@Level int level, @Nullable Marker marker, @Nullable Object message) {
        var event = LOGGER.atLevel(toLogLevel(level));
        if (marker != null) {
            event = event.addMarker(marker);
        }
        event.setMessage(String.valueOf(message)).log();
    }

    /**
     * Logs a lazily constructed message. The supplier is called only if the logging level is enabled.
     *
     * @param level           The logging level.
     * @param marker          Used to identify the source of a log message.
     * @param messageSupplier A supplier that produces a log message when called.
     */
    public static void println(@Level int level, @Nullable Marker marker, @Nullable Supplier<?> messageSupplier) {
        var event = LOGGER.atLevel(toLogLevel(level));
        if (marker != null) {
            event = event.addMarker(marker);
        }
        event.setMessage(() -> String.valueOf(messageSupplier != null ? messageSupplier.get() : null)).log();
    }

    private static org.slf4j.event.Level toLogLevel(int level) {
        return switch (level) {
            case VERBOSE -> org.slf4j.event.Level.TRACE;
            case DEBUG -> org.slf4j.event.Level.DEBUG;
            case INFO -> org.slf4j.event.Level.INFO;
            case WARN -> org.slf4j.event.Level.WARN;
            case ERROR, ASSERT -> org.slf4j.event.Level.ERROR;
            default -> {
                assert false;
                yield org.slf4j.event.Level.INFO;
            }
        };
    }
}
