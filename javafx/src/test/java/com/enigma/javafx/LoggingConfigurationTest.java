package com.enigma.javafx;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingConfigurationTest {
    @Test
    void configuresOneRollingFileAppenderUnderTheUserHomeDirectory() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root =
                context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var fileAppender = root.getAppender("FILE");

        assertEquals(Level.INFO, root.getLevel());
        assertNotNull(fileAppender);
        assertInstanceOf(RollingFileAppender.class, fileAppender);
        assertTrue(fileAppender.isStarted());

        RollingFileAppender<?> rollingFileAppender =
                (RollingFileAppender<?>) fileAppender;
        assertEquals(
                Path.of(System.getProperty("user.home"), ".gestor-enigma", "logs", "gestor-enigma.log"),
                Path.of(rollingFileAppender.getFile()));
    }
}
