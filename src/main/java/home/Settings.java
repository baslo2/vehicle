package home;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import home.gui.ColorSchema;

public final class Settings {

    public enum Setting {

        STYLE("style", ColorSchema.CROSSPLATFORM.name().toLowerCase(Locale.ROOT)),
        DB_FILE_PATH("db_file_path", IConsts.EMPTY_STRING);

        private final String name;
        private final String defaultValue;

        private Setting(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    private static final String SETTINGS_FILE_NAME = "settings.properties";
    private static final Properties SETTINGS = new Properties();

    public static String getStyle() {
        return get(Setting.STYLE);
    }

    public static String getDbFilePath() {
        return get(Setting.DB_FILE_PATH);
    }

    private static String get(Setting setting) {
        return SETTINGS.getProperty(setting.getName());
    }

    public static boolean hasPathToDBFile() {
        String dbFilePath = getDbFilePath();
        return dbFilePath != null && !dbFilePath.isBlank();
    }

    public static void writeSetting(Setting setting, String value) throws IOException {
        SETTINGS.setProperty(setting.getName(), value);
        try (var outputStream = new FileOutputStream(SETTINGS_FILE_NAME)) {
            SETTINGS.store(outputStream, null);
        } catch (IOException e) {
            throw new IllegalStateException("Error while filling the setting file: " + SETTINGS_FILE_NAME, e);
        }
        readSettings();
    }

    public static void readSettings() {
        try (var inputStream = new FileInputStream(getSettingsPath())) {
            SETTINGS.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Error while read setttings from file: "
                    + SETTINGS_FILE_NAME, e);
        }
    }

    public static String getSettingsPath() throws IOException {
        try {
            var file = new File(SETTINGS_FILE_NAME);
            if (!file.exists()) {
                file.createNewFile();
                fillWithDefaultSettings();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException("Error while creating the settings file: " + SETTINGS_FILE_NAME, e);
        }
    }

    private static void fillWithDefaultSettings() throws IOException {
        try (var outputStream = new FileOutputStream(SETTINGS_FILE_NAME)) {
            SETTINGS.setProperty(Setting.STYLE.getName(), Setting.STYLE.getDefaultValue());
            SETTINGS.setProperty(Setting.DB_FILE_PATH.getName(), Setting.DB_FILE_PATH.getDefaultValue());
            SETTINGS.store(outputStream, null);
        } catch (IOException e) {
            throw new IllegalStateException("Error while fill default settings: " + SETTINGS_FILE_NAME, e);
        }
    }

    private Settings() {
    }
}
