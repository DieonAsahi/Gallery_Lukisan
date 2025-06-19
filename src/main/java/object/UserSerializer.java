package object;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class UserSerializer {
    public static void serialize(User user) {
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream("user_" + user.getUsername() + ".ser"))) {
            out.writeObject(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
