package object;

import java.io.Serializable;

public class User implements Serializable {

    private String username;
    private String password;
    private String role;
    private String nama;
    private String passwordPlain;

    public User(String username, String password, String role, String nama) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.nama = nama;
        this.passwordPlain = password; // asumsikan awalnya sama
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return nama;
    }

    public void setUsername(String username) { // ✅ Setter
        this.username = username;
    }

    public void setName(String nama) { // ✅ Setter
        this.nama = nama;
    }

    public void setPassword(String password) { // ✅ Setter
        this.password = password;
    }

    private String bahasa = "en"; // default

    public String getBahasa() {
        return bahasa;
    }

    public void setBahasa(String bahasa) {
        this.bahasa = bahasa;
    }

    public String getPasswordPlain() {
        return passwordPlain;
    }

    public void setPasswordPlain(String passwordPlain) {
        this.passwordPlain = passwordPlain;
    }

}
