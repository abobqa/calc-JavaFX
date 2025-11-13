package app.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {
    private static HikariDataSource ds;

    public static void init(String host, int port, String db, String user, String pass) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + db);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setDriverClassName("org.postgresql.Driver");
        cfg.addDataSourceProperty("tcpKeepAlive", "true");
        ds = new HikariDataSource(cfg);
        runMigrations();
    }

    public static DataSource ds() { return ds; }

    private static void runMigrations() {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'role_enum') THEN
                    CREATE TYPE role_enum AS ENUM ('USER','ADMIN');
                  END IF;
                END$$;

                CREATE TABLE IF NOT EXISTS users (
                  id            BIGSERIAL PRIMARY KEY,
                  username      TEXT UNIQUE NOT NULL,
                  password_hash TEXT NOT NULL,
                  role          role_enum NOT NULL,
                  created_at    TIMESTAMPTZ DEFAULT now()
                );

                CREATE TABLE IF NOT EXISTS history (
                  id          BIGSERIAL PRIMARY KEY,
                  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                  calc_type   TEXT NOT NULL CHECK (calc_type IN ('OHM','DIVIDER')),
                  input_json  JSONB,
                  result_json JSONB,
                  created_at  TIMESTAMPTZ DEFAULT now()
                );

                UPDATE history SET input_json='{}'::jsonb WHERE input_json IS NULL;
                UPDATE history SET result_json='{}'::jsonb WHERE result_json IS NULL;

                ALTER TABLE history
                  ALTER COLUMN input_json TYPE jsonb USING COALESCE(input_json,'{}'::jsonb),
                  ALTER COLUMN result_json TYPE jsonb USING COALESCE(result_json,'{}'::jsonb);

                ALTER TABLE history
                  ALTER COLUMN input_json SET NOT NULL,
                  ALTER COLUMN result_json SET NOT NULL;

                CREATE INDEX IF NOT EXISTS idx_hist_user_created
                  ON history(user_id, created_at DESC);
                CREATE INDEX IF NOT EXISTS idx_hist_input_gin
                  ON history USING GIN (input_json);
                """);
        } catch (SQLException e) {
            throw new RuntimeException("DB migration failed", e);
        }
    }
}
