package org.flowant.stats;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Logger;

import org.flowant.function.FunctionT;

public class Config {
    private static Logger logger = Logger.getLogger(Config.class.getSimpleName());

    public static final String CLASSPATH_LOG_PROPERTIES = "/logging.properties";
    public static final String CLASSPATH_CONFIG_PROPERTIES = "/config.properties";
    public static final String SEP_DIR = FileSystems.getDefault().getSeparator();

    /**
     * keys on config.properties file
     */
    public static enum Keys {
        CollectStartYearMonth,
        AnalysisStartYearMonth,
        ServiceKeyNationItemTrade,
        MongoClientUri,
        ChartImageOutputPath,
        ChartScreenWidth,
        ChartScreenHeight,
        ChartImageWidth,
        ChartImageHeight,
        ChartFont,
        ChartLineWidth,
        SearchUrlPrefix,
        SearchUrlSuffix,
        TradeFileRepositoryPath;
    }

    private static Properties p;

    public static void load() throws IOException {
        p = load(CLASSPATH_CONFIG_PROPERTIES);
        logger.config(() -> "config properties:" + Config.p.toString());
    }

    private static Properties load(String path) throws IOException {
        try (InputStream is = Config.class.getResourceAsStream(path)) {
            Properties prop = new Properties();
            prop.load(is);
            logger.config(() -> CLASSPATH_CONFIG_PROPERTIES + " is loaded");
            return prop;
        }
    }

    public static String getConfig(String name, String defValue) {
        return ofNullable(p.getProperty(name)).orElse(defValue);
    }

    public static String getConfig(Keys name, String defValue) {
        return getConfig(name.name(), defValue);
    }

    public static String getConfig(String name) {
        return of(p.getProperty(name)).get();
    }

    public static String getConfig(Keys name) {
        return getConfig(name.name());
    }

    private static <T> T getConfig(String name, T defValue, Function<String, Optional<T>> parser) {
        return ofNullable(p.getProperty(name)).flatMap(parser).orElse(defValue);
    }

    private static <T> T getConfig(String name, Function<String, Optional<T>> parser) {
        return of(p.getProperty(name)).flatMap(parser).get();
    }

    public static int getConfigInt(String name, int defValue) {
        return getConfig(name, defValue, (s) -> of(Integer.valueOf(s)));
    }

    public static int getConfigInt(Keys name, int defValue) {
        return getConfigInt(name.name(), defValue);
    }

    public static int getConfigInt(String name) {
        return getConfig(name, (s) -> of(Integer.valueOf(s)));
    }

    public static int getConfigInt(Keys name) {
        return getConfigInt(name.name());
    }

    /**
     * @param name
     * @param clazz
     *            Number 하위 클래스들 중 valueOf 메소드를 가진 클래스 타입을 대상으로 동작함. valueOf를
     *            가지지 않은 Number 클래스를 인자로 전달하게 되면 예외가 발생함.
     * @return
     * @see ConfigTest
     */
    public static <T extends Number> T getConfig(String name, Class<T> clazz) {
        FunctionT<String, Optional<T>> parse;
        parse = s -> of(clazz.cast(clazz.getDeclaredMethod("valueOf", String.class).invoke(Optional.empty(), s)));
        return getConfig(name, parse);
    }

    public static <T extends Number> T getConfig(Keys name, Class<T> clazz) {
        return getConfig(name.name(), clazz);
    }

}
