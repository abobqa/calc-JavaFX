package app.db;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class HistoryRepo {
    private final DataSource ds;
    public HistoryRepo(DataSource ds){ this.ds = ds; }

    /**
     * Сохранение истории для калькулятора Ома.
     * JSON собирается на стороне PostgreSQL через jsonb_build_object — никакой строковой сериализации в Java.
     * SQL NULL в параметрах превращается в JSON null внутри объектов.
     */
    public void saveOhm(long userId,
                        Double vIn, Double iIn, Double rIn,
                        Double vOut, Double iOut, Double rOut) {
        String sql = """
            INSERT INTO history (user_id, calc_type, input_json, result_json)
            VALUES (
              ?, 'OHM',
              jsonb_build_object('V',   ?, 'I',   ?, 'R',   ?),
              jsonb_build_object('Vout',?, 'Iout',?, 'Rout',?)
            )
        """;
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);

            // Входные
            if (vIn  != null) ps.setDouble(2, vIn);  else ps.setNull(2,  Types.DOUBLE);
            if (iIn  != null) ps.setDouble(3, iIn);  else ps.setNull(3,  Types.DOUBLE);
            if (rIn  != null) ps.setDouble(4, rIn);  else ps.setNull(4,  Types.DOUBLE);

            // Результаты
            if (vOut != null) ps.setDouble(5, vOut); else ps.setNull(5, Types.DOUBLE);
            if (iOut != null) ps.setDouble(6, iOut); else ps.setNull(6, Types.DOUBLE);
            if (rOut != null) ps.setDouble(7, rOut); else ps.setNull(7, Types.DOUBLE);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("history.saveOhm failed", e);
        }
    }

    /**
     * Общий метод для других калькуляторов (например, DIVIDER).
     * Усилен: COALESCE(NULLIF(?,''))::jsonb гарантирует {}, если пришла пустая строка или null.
     */
    public void save(long userId, String calcType, String inputJson, String resultJson){
        String sql = """
            INSERT INTO history(user_id, calc_type, input_json, result_json)
            VALUES (?, ?,
                    COALESCE(NULLIF(?,'')::jsonb, '{}'::jsonb),
                    COALESCE(NULLIF(?,'')::jsonb, '{}'::jsonb))
        """;
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, calcType);
            ps.setString(3, inputJson);
            ps.setString(4, resultJson);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("history.save failed", e); }
    }

    public List<HistoryRow> list(Role role, long userId){
        String sql = (role==Role.ADMIN)
                ? "SELECT id, user_id, calc_type, input_json::text, result_json::text, created_at FROM history ORDER BY created_at DESC LIMIT 500"
                : "SELECT id, user_id, calc_type, input_json::text, result_json::text, created_at FROM history WHERE user_id=? ORDER BY created_at DESC LIMIT 500";
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            if (role!=Role.ADMIN) ps.setLong(1, userId);
            try (var rs = ps.executeQuery()) {
                var list = new ArrayList<HistoryRow>();
                while (rs.next()){
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<HistoryRow> listFiltered(Role role, long userId, String calcType, Instant from, Instant to){
        StringBuilder sb = new StringBuilder();
        List<Object> params = new ArrayList<>();
        if (role == Role.ADMIN) {
            sb.append("SELECT id, user_id, calc_type, input_json::text, result_json::text, created_at FROM history WHERE 1=1");
        } else {
            sb.append("SELECT id, user_id, calc_type, input_json::text, result_json::text, created_at FROM history WHERE user_id=?");
            params.add(userId);
        }
        if (calcType != null && !calcType.isBlank() && !"ALL".equalsIgnoreCase(calcType)) {
            sb.append(" AND calc_type=?");
            params.add(calcType);
        }
        if (from != null) {
            sb.append(" AND created_at >= ?");
            params.add(Timestamp.from(from));
        }
        if (to != null) {
            sb.append(" AND created_at < ?");
            params.add(Timestamp.from(to));
        }
        sb.append(" ORDER BY created_at DESC LIMIT 1000");

        try (var c = ds.getConnection(); var ps = c.prepareStatement(sb.toString())) {
            for (int i=0; i<params.size(); i++) {
                ps.setObject(i+1, params.get(i));
            }
            try (var rs = ps.executeQuery()) {
                var list = new ArrayList<HistoryRow>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private HistoryRow map(ResultSet rs) throws SQLException {
        return new HistoryRow(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("calc_type"),
                rs.getString("input_json"),
                rs.getString("result_json"),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    public record HistoryRow(long id, long userId, String calcType, String inputJson, String resultJson, Instant createdAt){}
}
