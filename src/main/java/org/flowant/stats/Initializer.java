package org.flowant.stats;

import static org.flowant.stats.Config.CLASSPATH_LOG_PROPERTIES;
import static org.flowant.util.FileUtil.mkdirs;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Initializer {

    static Logger logger;

    public static void Initialize() throws IOException {
        loadLogProperties();
        Config.load();
        checkEncoding();
        checkFont();
    }

    public static void checkEncoding() {
        logger.info("Charset.defaultCharset:" + Charset.defaultCharset());
        logger.info("file.encoding:" + System.getProperty("file.encoding"));
    }

    public static void checkFont() {
        GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String fontNames = Stream.of(e.getAllFonts()).map(Font::getFontName).collect(Collectors.joining(", "));
        logger.info("GraphicsEnvironment fonts: " + fontNames);
    }

    public static void loadLogProperties() throws IOException {
        LogManager logManager = LogManager.getLogManager();
        try (InputStream is = Initializer.class.getResourceAsStream(CLASSPATH_LOG_PROPERTIES)) {
            logManager.readConfiguration(is);
        }
        String pattern = logManager.getProperty("java.util.logging.FileHandler.pattern");
        if (Objects.nonNull(pattern)) {
            mkdirs(Paths.get(pattern).getParent());
        }
        logger = Logger.getLogger(Initializer.class.getSimpleName());
        logger.config(() -> CLASSPATH_LOG_PROPERTIES + " is loaded");
    }

    public static void main(String[] args) throws IOException {
        Initializer.Initialize();
    }
}
