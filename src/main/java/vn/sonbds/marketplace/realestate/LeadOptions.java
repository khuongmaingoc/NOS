package vn.sonbds.marketplace.realestate;

import java.util.List;

public final class LeadOptions {
    public static final List<String> SOURCES = List.of("Fanpage", "TikTok");
    public static final List<String> SUBJECTS = List.of("Chủ sở hữu", "Môi giới", "Loại");
    public static final List<String> NEEDS = List.of("Cho thuê", "Bán");
    public static final List<String> GENDERS = List.of("Nam", "Nữ");
    public static final List<String> CHANNELS = List.of("Profile FB", "Zalo", "TikTok");
    public static final List<String> INTEREST_LEVELS = List.of("Đồng ý", "Đang cân nhắc", "Chưa xác định", "Không đồng ý");
    public static final List<String> CONTACT_STATUSES = List.of(
            "Chưa liên hệ được", "Đã liên hệ", "Đang trao đổi", "Đang chăm sóc",
            "Cần gọi lại", "Hẹn gọi lại", "Sai số", "Không liên hệ");
    public static final List<String> PROPERTY_FEATURES = List.of("1PN", "1PN+1", "2PN");
    public static final List<String> DIRECTIONS = List.of("Tây Bắc", "Đông Bắc", "Đông Nam", "Tây Nam");
    public static final List<String> POSITIONS = List.of("Thường", "Góc");
    public static final List<String> INTERIORS = List.of("Bàn giao thô", "Nguyên bản", "Cơ bản", "Full nội thất", "Khác");
    public static final List<String> LEGAL_STATUSES = List.of("Sở hữu lâu dài");
    public static final List<String> PRICE_UNITS = List.of("$", "VND");
    public static final List<String> PROPERTY_STATUSES = List.of(
            "Chưa sẵn sàng", "Sẵn sàng", "Đang lock", "Đã đặt cọc", "Đã cho thuê", "Đã bán");
    public static final List<String> DEAL_STATUSES = List.of("Thành công", "Thất bại");

    private LeadOptions() {
    }
}
