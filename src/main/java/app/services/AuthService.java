package app.services;

import app.db.Role;
import app.db.Session;
import app.db.UserRepo;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Optional;

public class AuthService {
    private final UserRepo repo;
    public AuthService(UserRepo repo){ this.repo = repo; }

    public boolean register(String username, String rawPassword, Role role){
        if (repo.findByUsername(username).isPresent()) return false;
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        long id = repo.insert(username, hash, role);
        return id > 0;
    }

    public boolean login(String username, String rawPassword){
        Optional<UserRepo.UserRow> row = repo.findByUsername(username);
        if (row.isPresent() && BCrypt.checkpw(rawPassword, row.get().passwordHash())) {
            Session.set(row.get().id(), row.get().username(), row.get().role());
            return true;
        }
        return false;
    }

    public void logout(){ Session.clear(); }
}
