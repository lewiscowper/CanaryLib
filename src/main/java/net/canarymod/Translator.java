package net.canarymod;

import net.canarymod.config.Configuration;
import net.visualillusionsent.utils.FileUtils;
import net.visualillusionsent.utils.LocaleHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static net.canarymod.Canary.log;

/**
 * This class handles internationalization (aka i18n).
 * It will look up translations from a given key to the language that is currently active.
 *
 * @author Chris (damagefilter)
 * @author Jason (darkdiplomat)
 */
public class Translator extends LocaleHelper {
    private static final String canaryLang = "lang/canary/"; //allow plugins to borrow the lang directory for their lang files
    private static final boolean doUpdate = Configuration.getServerConfig().updateLang();
    private static final String[] locales = new String[]{ // The Default Supported
                                                                "en_US", "da_DK", "nl_NL", "fi_FI", "fr_FR", "de_DE", "it_IT", "no_NO",
                                                                "pl_PL", "en_PT", "ru_RU", "es_ES", "sv_SE"
    };
    private static final Translator instance;

    static {
        checkLangFiles();
        instance = new Translator();
    }

    private Translator() {
        super(true, canaryLang, Configuration.getServerConfig().getServerLocale());
    }

    /**
     * Translates a message into the current system language if that mapping exists
     *
     * @param key
     *         the message key to used to get the message from the Language file
     * @param locale
     *         the locale code to get translation for
     *
     * @return the translated message
     */
    public static String localTranslate(String key, String locale) {
        return instance.localeTranslate(key, locale);
    }

    /**
     * Translates the given message key and applies formatting according to standard Java formatting rules
     *
     * @param key
     *         the message key to used to get the message from the Language file
     * @param locale
     *         the locale code to get translation for
     * @param args
     *         the arguments used to format the message
     *
     * @return the translated message
     *
     * @see java.text.MessageFormat
     */
    public static String localTranslate(String key, String locale, Object... args) {
        return instance.localeTranslate(key, locale, args);
    }

    /**
     * Translates a message into the current system language if that mapping exists
     *
     * @param messageKey
     *         the message key to used to get the message from the Language file
     *
     * @return the translated message
     */
    public static String translate(String messageKey) {
        return instance.systemTranslate(messageKey);
    }

    /**
     * Translates the given message key and applies formatting according to standard Java formatting rules
     *
     * @param messageKey
     *         the message key to used to get the message from the Language file
     * @param format
     *         the arguments used to format the message
     *
     * @return the translated message
     *
     * @see java.text.MessageFormat
     */
    public static String translateAndFormat(String messageKey, Object... format) {
        return instance.systemTranslate(messageKey, format);
    }

    /**
     * Uses Minecraft's native translation to translate a key
     *
     * @param key
     *         the key to translate
     *
     * @return the translated string
     */
    public static String nativeTranslate(String key) {
        return NativeTranslateBridge.$.nativeTranslate(key);
    }

    /**
     * Uses Minecraft's native translation to translate a key
     *
     * @param key
     *         the key to translate
     * @param args
     *         the argument objects to use with translation
     *
     * @return the translated string
     */
    public static String nativeTranslate(String key, Object... args) {
        return NativeTranslateBridge.$.nativeTranslate(key, args);
    }

    /**
     * Checks if a key exists for translation in Minecraft's native translation
     *
     * @param key
     *         the key to check
     *
     * @return {@code true} if exists; {@code false} if not
     */
    public static boolean nativeCanTranslate(String key) {
        return NativeTranslateBridge.$.nativeCanTranslate(key);
    }

    /**
     * Returns the instance for this Translator.
     * For translation purposes, please use the provided static methods.
     * This here is for working with the command system
     *
     * @return the Translator instance
     */
    public static Translator getInstance() {
        return instance;
    }

    private static void checkLangFiles() {
        File directory = new File(canaryLang);
        String langTXT = "languages.txt";
        List<String> fileNames = Arrays.asList(directory.isDirectory() ? directory.list() : new String[0]);

        if (!directory.exists() && directory.mkdirs()) {
            moveLangFile(langTXT);
        }
        else if (!fileNames.contains("languages.txt")) {
            moveLangFile(langTXT);
        }
        else {
            checkSumLang(langTXT);
        }

        for (String locale : locales) {
            String locLang = locale.concat(".lang");
            if (!fileNames.contains(locLang)) {
                moveLangFile(locLang);
            }
            else {
                checkSumLang(locLang);
            }
        }
    }

    private static void moveLangFile(String locale) {
        try {
            FileUtils.cloneFileFromJar(Canary.getCanaryJarPath(), "resources/lang/".concat(locale), canaryLang.concat(locale));
        }
        catch (IOException ioex) {
            log.error("Failed to transfer lang file for locale: ".concat(locale.replace(".lang", "")));
        }
    }

    private static void checkSumLang(String locale) {
        if (doUpdate) {
            try {
                if (!FileUtils.md5SumMatch(new FileInputStream(canaryLang.concat(locale)), Translator.class.getResourceAsStream("/resources/lang/".concat(locale)))) {
                    // Checksums don't match? move file
                    moveLangFile(locale);
                }
            }
            catch (Exception ex) {
                log.error("Language File Checksum failed...", ex);
            }
        }
    }
}
