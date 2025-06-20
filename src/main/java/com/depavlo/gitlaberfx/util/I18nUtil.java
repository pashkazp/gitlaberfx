package com.depavlo.gitlaberfx.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utility class for internationalization (i18n) support.
 * Provides methods for accessing localized messages from resource bundles.
 */
public class I18nUtil {
    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static Locale currentLocale = new Locale("en", "US"); // Default to English
    private static ResourceBundle resourceBundle;

    /**
     * Gets the base name of the resource bundle.
     * 
     * @return The base name of the resource bundle
     */
    public static String getBundleBaseName() {
        return BUNDLE_BASE_NAME;
    }

    static {
        // Initialize the resource bundle with the default locale
        loadResourceBundle();
    }

    /**
     * Loads the resource bundle for the current locale.
     */
    private static void loadResourceBundle() {
        resourceBundle = ResourceBundle.getBundle(BUNDLE_BASE_NAME, currentLocale);
    }

    /**
     * Sets the current locale and reloads the resource bundle.
     * 
     * @param locale The locale to set
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        loadResourceBundle();
    }

    /**
     * Gets the current locale.
     * 
     * @return The current locale
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Gets a localized message for the given key.
     * 
     * @param key The message key
     * @return The localized message
     */
    public static String getMessage(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Exception e) {
            // If the key is not found, return the key itself as a fallback
            return key;
        }
    }

    /**
     * Gets a localized message for the given key with parameters.
     * Parameters in the message are represented by {0}, {1}, etc.
     * 
     * @param key The message key
     * @param params The parameters to substitute in the message
     * @return The localized message with parameters substituted
     */
    public static String getMessage(String key, Object... params) {
        try {
            String message = resourceBundle.getString(key);
            for (int i = 0; i < params.length; i++) {
                message = message.replace("{" + i + "}", params[i].toString());
            }
            return message;
        } catch (Exception e) {
            // If the key is not found, return the key itself as a fallback
            return key;
        }
    }
}
