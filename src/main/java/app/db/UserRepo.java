package app.db;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

public class UserRepo {
    private final DataSource ds;
    public UserRepo(DataSource ds){ this.ds = ds; }

    public Optional<UserRow> findByUsername(String username){
        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?";
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new UserRow(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            Role.valueOf(rs.getString("role"))
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public long insert(String username, String passwordHash, Role role){
        String sql = "INSERT INTO users(username, password_hash, role) VALUES (?,?,?::role_enum) RETURNING id";
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            try (var rs = ps.executeQuery()) {
                rs.next(); return rs.getLong(1);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public record UserRow(long id, String username, String passwordHash, Role role){}
}
