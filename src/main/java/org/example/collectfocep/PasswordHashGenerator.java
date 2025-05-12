import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        // Créer un encodeur Argon2 avec les mêmes paramètres que ceux de votre application
        // Paramètres: saltLength, hashLength, parallelism, memory, iterations
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 10);

        // Mot de passe que vous voulez hacher
        String plainPassword = "password";

        // Générer le hachage
        String hashedPassword = encoder.encode(plainPassword);

        System.out.println("Mot de passe en clair: " + plainPassword);
        System.out.println("Mot de passe haché: " + hashedPassword);
    }
}