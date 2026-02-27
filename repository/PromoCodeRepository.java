package by.academy.project.repository;

import by.academy.project.exception.PromoCodeException;
import by.academy.project.models.PromoCode;
import by.academy.project.service.infra.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PromoCodeRepository implements PromoCodeRepositoryInterface {

    public boolean createPromoCode(PromoCode promoCode) throws PromoCodeException {
        String sql = "INSERT INTO promo_codes (code, owner_user_id, discount_percent, max_uses, " +
                "used_count, created_at, expires_at, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, promoCode.getCode());
            stmt.setInt(2, promoCode.getOwnerUserId());
            stmt.setDouble(3, promoCode.getDiscountPercent());
            stmt.setInt(4, promoCode.getMaxUses());
            stmt.setInt(5, promoCode.getUsedCount());
            stmt.setTimestamp(6, Timestamp.valueOf(promoCode.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(promoCode.getExpiresAt()));
            stmt.setBoolean(8, promoCode.isActive());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        promoCode.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new PromoCodeException("Ошибка создания промокода: " + e.getMessage(), e);
        }
        return false;
    }

    public PromoCode getPromoCodeByCode(String code) throws PromoCodeException {
        String sql = "SELECT * FROM promo_codes WHERE code = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPromoCode(rs);
                }
            }
        } catch (SQLException e) {
            throw new PromoCodeException("Ошибка получения промокода: " + e.getMessage(), e);
        }
        return null;
    }

    public List<PromoCode> getActivePromoCodesByUserId(int userId) throws PromoCodeException {
        List<PromoCode> promoCodes = new ArrayList<>();
        String sql = "SELECT * FROM promo_codes WHERE owner_user_id = ? AND is_active = true " +
                "ORDER BY created_at DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    promoCodes.add(mapResultSetToPromoCode(rs));
                }
            }
        } catch (SQLException e) {
            throw new PromoCodeException("Ошибка получения промокодов: " + e.getMessage(), e);
        }
        return promoCodes;
    }

    public boolean incrementUsedCount(int promoCodeId) throws PromoCodeException {
        String sql = "UPDATE promo_codes SET used_count = used_count + 1 WHERE id = ? AND used_count < max_uses";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, promoCodeId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PromoCodeException("Ошибка обновления счетчика использований: " + e.getMessage(), e);
        }
    }

    public void deactivatePromoCode(int promoCodeId) throws PromoCodeException {
        String sql = "UPDATE promo_codes SET is_active = false WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, promoCodeId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new PromoCodeException("Ошибка деактивации промокода: " + e.getMessage(), e);
        }
    }

    public void deactivateExpiredPromoCodes() throws PromoCodeException {
        String sql = "UPDATE promo_codes SET is_active = false WHERE expires_at < NOW() AND is_active = true";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new PromoCodeException("Ошибка деактивации просроченных промокодов: " + e.getMessage(), e);
        }
    }

    public int getPromoCodeCountByUserIdSince(int userId, LocalDateTime since) {
        String sql = "SELECT COUNT(*) FROM promo_codes WHERE owner_user_id = ? AND created_at >= ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(since));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Ошибка подсчета промокодов: " + e.getMessage());
        }
        return 0;
    }

    private PromoCode mapResultSetToPromoCode(ResultSet rs) throws SQLException {
        PromoCode promoCode = new PromoCode();
        promoCode.setId(rs.getInt("id"));
        promoCode.setCode(rs.getString("code"));
        promoCode.setOwnerUserId(rs.getInt("owner_user_id"));
        promoCode.setDiscountPercent(rs.getDouble("discount_percent"));
        promoCode.setMaxUses(rs.getInt("max_uses"));
        promoCode.setUsedCount(rs.getInt("used_count"));
        promoCode.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        promoCode.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        promoCode.setActive(rs.getBoolean("is_active"));
        return promoCode;
    }
}