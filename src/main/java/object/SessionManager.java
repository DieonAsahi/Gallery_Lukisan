package object;

public class SessionManager {

    private static final DataManager<User> session = new DataManager<>();
    private static String languageCode = "en"; // default bahasa Inggris

    public static void set(User user) {
        session.ambilSemua().clear(); // Hapus user lama
        session.tambah(user);         // Simpan user baru

        // Sinkronkan bahasa jika sebelumnya sudah dipilih
        if (user != null) {
            user.setBahasa(languageCode);
        }
    }

    public static User get() {
        return session.ambilSemua().isEmpty() ? null : session.ambilSemua().get(0);
    }

    public static void clear() {
        session.ambilSemua().clear();
        languageCode = "en"; // reset juga bahasanya ke default
    }

    public static void setLanguage(String lang) {
        languageCode = lang;

        // Jika user sudah login, perbarui juga field bahasa-nya
        User u = get();
        if (u != null) {
            u.setBahasa(lang);
        }
    }

    public static String getLanguage() {
        return languageCode;
    }
}
