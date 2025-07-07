package object;

import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageManager {

    private static ResourceBundle bundle;

    public static void init(String languageCode) {
        Locale locale;
        switch (languageCode) {
            case "id" -> locale = new Locale("id", "ID");
            case "en" -> locale = new Locale("en", "US");
            default -> locale = Locale.getDefault();
        }
        bundle = ResourceBundle.getBundle("localization.Bundle", locale);
    }

    public static ResourceBundle getBundle() {
        if (bundle == null) {
            init("en");
        }
        return bundle;
    }

    // (Opsional) @Deprecated, bisa diganti pemanggilan ke getBundle()
    @Deprecated
    public static ResourceBundle getMessages() {
        return getBundle();
    }
}
