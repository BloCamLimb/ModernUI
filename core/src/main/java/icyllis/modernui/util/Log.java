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
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.Configurator;
import org.intellij.lang.annotations.MagicConstant;
import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Framework wrapper over Log4j API for sending log output.
 *
 * @since 3.10
 */
public final class Log {

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static final Logger LOGGER = LogManager.getLogger(ModernUI.NAME_CPT);

    /**
     * @hidden
     */
    @MagicConstant(intValues = {FATAL, ERROR, WARN, INFO, DEBUG, TRACE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Level {
    }

    /**
     * A fine-grained debug message, typically capturing the flow through the application.
     */
    public static final int TRACE = 2;

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
     * A fatal event that will prevent the application from continuing.
     */
    public static final int FATAL = 7;

    private Log() {
    }

    /**
     * Logs a message object with the {@link #TRACE} level.
     *
     * @param tag used to identify the source of a log message.
     * @param msg the message string to log.
     */
    public static void trace(@Nullable String tag, @NonNull String msg) {
        if (tag == null) {
            LOGGER.trace(msg);
        } else {
            LOGGER.trace(MarkerManager.getMarker(tag), msg);
        }
    }

    /**
     * Logs a message object with the {@link #TRACE} level.
     *
     * @param tag    used to identify the source of a log message.
     * @param msg    the message string to log.
     * @param params parameters to the message.
     */
    public static void trace(@Nullable String tag, @NonNull String msg, Object... params) {
        if (tag == null) {
            LOGGER.trace(msg, params);
        } else {
            LOGGER.trace(MarkerManager.getMarker(tag), msg, params);
        }
    }

    /**
     * Logs a message at the {@link #TRACE} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param tag       used to identify the source of a log message.
     * @param msg       the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void trace(@Nullable String tag, @NonNull String msg, Throwable throwable) {
        if (tag == null) {
            LOGGER.trace(msg, throwable);
        } else {
            LOGGER.trace(MarkerManager.getMarker(tag), msg, throwable);
        }
    }

    /**
     * Logs a message object with the {@link #DEBUG} level.
     *
     * @param tag used to identify the source of a log message.
     * @param msg the message string to log.
     */
    public static void debug(@Nullable String tag, @NonNull String msg) {
        if (tag == null) {
            LOGGER.debug(msg);
        } else {
            LOGGER.debug(MarkerManager.getMarker(tag), msg);
        }
    }

    /**
     * Logs a message object with the {@link #DEBUG} level.
     *
     * @param tag    used to identify the source of a log message.
     * @param msg    the message string to log.
     * @param params parameters to the message.
     */
    public static void debug(@Nullable String tag, @NonNull String msg, Object... params) {
        if (tag == null) {
            LOGGER.debug(msg, params);
        } else {
            LOGGER.debug(MarkerManager.getMarker(tag), msg, params);
        }
    }

    /**
     * Logs a message at the {@link #DEBUG} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param tag       used to identify the source of a log message.
     * @param msg       the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void debug(@Nullable String tag, @NonNull String msg, Throwable throwable) {
        if (tag == null) {
            LOGGER.debug(msg, throwable);
        } else {
            LOGGER.debug(MarkerManager.getMarker(tag), msg, throwable);
        }
    }

    /**
     * Logs a message object with the {@link #INFO} level.
     *
     * @param tag used to identify the source of a log message.
     * @param msg the message string to log.
     */
    public static void info(@Nullable String tag, @NonNull String msg) {
        if (tag == null) {
            LOGGER.info(msg);
        } else {
            LOGGER.info(MarkerManager.getMarker(tag), msg);
        }
    }

    /**
     * Logs a message object with the {@link #INFO} level.
     *
     * @param tag    used to identify the source of a log message.
     * @param msg    the message string to log.
     * @param params parameters to the message.
     */
    public static void info(@Nullable String tag, @NonNull String msg, Object... params) {
        if (tag == null) {
            LOGGER.info(msg, params);
        } else {
            LOGGER.info(MarkerManager.getMarker(tag), msg, params);
        }
    }

    /**
     * Logs a message at the {@link #INFO} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param tag       used to identify the source of a log message.
     * @param msg       the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void info(@Nullable String tag, @NonNull String msg, Throwable throwable) {
        if (tag == null) {
            LOGGER.info(msg, throwable);
        } else {
            LOGGER.info(MarkerManager.getMarker(tag), msg, throwable);
        }
    }

    /**
     * Logs a message object with the {@link #WARN} level.
     *
     * @param tag used to identify the source of a log message.
     * @param msg the message string to log.
     */
    public static void warn(@Nullable String tag, @NonNull String msg) {
        if (tag == null) {
            LOGGER.warn(msg);
        } else {
            LOGGER.warn(MarkerManager.getMarker(tag), msg);
        }
    }

    /**
     * Logs a message object with the {@link #WARN} level.
     *
     * @param tag    used to identify the source of a log message.
     * @param msg    the message string to log.
     * @param params parameters to the message.
     */
    public static void warn(@Nullable String tag, @NonNull String msg, Object... params) {
        if (tag == null) {
            LOGGER.warn(msg, params);
        } else {
            LOGGER.warn(MarkerManager.getMarker(tag), msg, params);
        }
    }

    /**
     * Logs a message at the {@link #WARN} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param tag       used to identify the source of a log message.
     * @param msg       the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void warn(@Nullable String tag, @NonNull String msg, Throwable throwable) {
        if (tag == null) {
            LOGGER.warn(msg, throwable);
        } else {
            LOGGER.warn(MarkerManager.getMarker(tag), msg, throwable);
        }
    }

    /**
     * Logs a message object with the {@link #ERROR} level.
     *
     * @param tag used to identify the source of a log message.
     * @param msg the message string to log.
     */
    public static void error(@Nullable String tag, @NonNull String msg) {
        if (tag == null) {
            LOGGER.error(msg);
        } else {
            LOGGER.error(MarkerManager.getMarker(tag), msg);
        }
    }

    /**
     * Logs a message object with the {@link #ERROR} level.
     *
     * @param tag    used to identify the source of a log message.
     * @param msg    the message string to log.
     * @param params parameters to the message.
     */
    public static void error(@Nullable String tag, @NonNull String msg, Object... params) {
        if (tag == null) {
            LOGGER.error(msg, params);
        } else {
            LOGGER.error(MarkerManager.getMarker(tag), msg, params);
        }
    }

    /**
     * Logs a message at the {@link #ERROR} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param tag       used to identify the source of a log message.
     * @param msg       the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void error(@Nullable String tag, @NonNull String msg, Throwable throwable) {
        if (tag == null) {
            LOGGER.error(msg, throwable);
        } else {
            LOGGER.error(MarkerManager.getMarker(tag), msg, throwable);
        }
    }

    /**
     * Logs a message object with the {@link #FATAL} level.
     *
     * @param tag used to identify the source of a log message.
     * @param msg the message string to log.
     */
    public static void fatal(@Nullable String tag, @NonNull String msg) {
        if (tag == null) {
            LOGGER.fatal(msg);
        } else {
            LOGGER.fatal(MarkerManager.getMarker(tag), msg);
        }
    }

    /**
     * Logs a message object with the {@link #FATAL} level.
     *
     * @param tag    used to identify the source of a log message.
     * @param msg    the message string to log.
     * @param params parameters to the message.
     */
    public static void fatal(@Nullable String tag, @NonNull String msg, Object... params) {
        if (tag == null) {
            LOGGER.fatal(msg, params);
        } else {
            LOGGER.fatal(MarkerManager.getMarker(tag), msg, params);
        }
    }

    /**
     * Logs a message at the {@link #FATAL} level including the stack trace of the {@link Throwable}
     * <code>throwable</code> passed as parameter.
     *
     * @param tag       used to identify the source of a log message.
     * @param msg       the message string to log.
     * @param throwable the {@code Throwable} to log, including its stack trace.
     */
    public static void fatal(@Nullable String tag, @NonNull String msg, Throwable throwable) {
        if (tag == null) {
            LOGGER.fatal(msg, throwable);
        } else {
            LOGGER.fatal(MarkerManager.getMarker(tag), msg, throwable);
        }
    }

    /**
     * Logs a {@link Throwable} that has been caught at the {@link #ERROR} level.
     *
     * @param throwable the Throwable.
     */
    public static void catching(Throwable throwable) {
        LOGGER.catching(throwable);
    }

    /**
     * Logs a {@link Throwable} to be thrown at the {@link #ERROR} level.
     * This may be coded as:
     *
     * <pre>
     * throw Log.throwing(e);
     * </pre>
     *
     * @param <T>       the Throwable type.
     * @param throwable The Throwable.
     * @return the Throwable.
     */
    public static <T extends Throwable> T throwing(T throwable) {
        return LOGGER.throwing(throwable);
    }

    /**
     * Logs a formatted message using the specified format string and arguments.
     *
     * @param level  The logging level.
     * @param tag    Used to identify the source of a log message.
     * @param format The format String.
     * @param params Arguments specified by the format.
     */
    public static void printf(@Level int level, @Nullable String tag, @PrintFormat String format, Object... params) {
        var logLevel = toLogLevel(level);
        if (tag == null) {
            LOGGER.printf(logLevel, format, params);
        } else {
            LOGGER.printf(logLevel, MarkerManager.getMarker(tag), format, params);
        }
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    public static void setLevel(@Level int level) {
        Configurator.setLevel(LOGGER, toLogLevel(level));
    }

    private static org.apache.logging.log4j.Level toLogLevel(int level) {
        return switch (level) {
            case TRACE -> org.apache.logging.log4j.Level.TRACE;
            case DEBUG -> org.apache.logging.log4j.Level.DEBUG;
            case INFO -> org.apache.logging.log4j.Level.INFO;
            case WARN -> org.apache.logging.log4j.Level.WARN;
            case ERROR -> org.apache.logging.log4j.Level.ERROR;
            case FATAL -> org.apache.logging.log4j.Level.FATAL;
            default -> throw new IllegalArgumentException();
        };
    }
}
