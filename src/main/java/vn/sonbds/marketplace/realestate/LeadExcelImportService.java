package vn.sonbds.marketplace.realestate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class LeadExcelImportService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_ROWS = 5_000;
    private static final Map<String, String> HEADER_FIELDS = headerFields();
    private static final List<String> TEMPLATE_HEADERS = List.of(
            "Họ tên", "Số điện thoại", "Email", "Mã tin liên quan", "Nguồn dữ liệu", "Đối tượng",
            "Nhu cầu", "Giới tính", "Kênh tiếp cận", "Mức độ quan tâm", "Trạng thái liên hệ",
            "Đặc điểm BĐS", "Hướng", "Vị trí", "Nội thất", "Hồ sơ pháp lý", "Đơn vị giá",
            "Tình trạng BĐS", "Trạng thái giao dịch", "Ghi chú", "Địa chỉ/khu vực BĐS", "Vĩ độ", "Kinh độ");

    private final CustomerLeadRepository leads;
    private final PropertyListingRepository listings;
    private final Validator validator;

    public LeadExcelImportService(CustomerLeadRepository leads, PropertyListingRepository listings,
                                  Validator validator) {
        this.leads = leads;
        this.listings = listings;
        this.validator = validator;
    }

    @Transactional
    public ImportResult importFile(Long ownerId, MultipartFile file) {
        validateFile(file);
        try (InputStream input = file.getInputStream()) {
            Workbook workbook;
            try {
                workbook = WorkbookFactory.create(input);
            } catch (EncryptedDocumentException | POIXMLException | IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Không đọc được file Excel. Hãy tải mẫu và lưu lại dưới dạng .xlsx hoặc .xls.");
            }
            try (workbook) {
                if (workbook.getNumberOfSheets() == 0) {
                    throw new IllegalArgumentException("File Excel không có trang tính.");
                }
                return importSheet(ownerId, workbook.getSheetAt(0));
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Không đọc được file Excel. Hãy tải mẫu và lưu lại dưới dạng .xlsx hoặc .xls.");
        }
    }

    public static List<String> templateHeaders() {
        return TEMPLATE_HEADERS;
    }

    private ImportResult importSheet(Long ownerId, Sheet sheet) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new IllegalArgumentException("Trang tính chưa có dòng tiêu đề.");
        }

        DataFormatter formatter = new DataFormatter(Locale.forLanguageTag("vi-VN"));
        Map<String, Integer> columns = readHeaders(header, formatter);
        if (!columns.containsKey("name")) {
            throw new IllegalArgumentException("Thiếu cột bắt buộc “Họ tên” trong dòng tiêu đề.");
        }
        if (sheet.getLastRowNum() > MAX_ROWS) {
            throw new IllegalArgumentException("File vượt quá giới hạn " + MAX_ROWS + " dòng dữ liệu.");
        }

        Set<String> seenContacts = new HashSet<>();
        leads.findByOwnerIdOrderByUpdatedAtDesc(ownerId).forEach(lead -> addContactKeys(seenContacts,
                lead.getPhone(), lead.getEmail()));
        Map<Long, PropertyListing> ownedListings = new HashMap<>();
        listings.findByOwnerIdOrderByCreatedAtDesc(ownerId)
                .forEach(listing -> ownedListings.put(listing.getId(), listing));

        List<CustomerLead> imported = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        int duplicatesSkipped = 0;
        int nonEmptyRows = 0;

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isEmpty(row, formatter)) continue;
            nonEmptyRows++;
            try {
                LeadRequest request = readLead(row, columns, formatter);
                validate(request);
                if (request.listingId() != null && !ownedListings.containsKey(request.listingId())) {
                    throw new IllegalArgumentException("Mã tin liên quan không thuộc tài khoản hiện tại.");
                }

                Set<String> contactKeys = contactKeys(request.phone(), request.email());
                if (contactKeys.stream().anyMatch(seenContacts::contains)) {
                    duplicatesSkipped++;
                    continue;
                }
                seenContacts.addAll(contactKeys);
                imported.add(new CustomerLead(ownerId, request.listingId(), request));
            } catch (IllegalArgumentException exception) {
                errors.add(new RowError(rowIndex + 1, exception.getMessage()));
            }
        }

        if (nonEmptyRows == 0) {
            throw new IllegalArgumentException("File không có dòng dữ liệu nào để nhập.");
        }
        if (!imported.isEmpty()) leads.saveAll(imported);
        return new ImportResult(imported.size(), duplicatesSkipped, errors);
    }

    private static Map<String, Integer> readHeaders(Row header, DataFormatter formatter) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (Cell cell : header) {
            String value = normalizeHeader(formatter.formatCellValue(cell));
            if (value.isBlank()) continue;
            String field = HEADER_FIELDS.get(value);
            if (field == null) continue;
            if (columns.putIfAbsent(field, cell.getColumnIndex()) != null) {
                throw new IllegalArgumentException("File có nhiều hơn một cột cho trường “"
                        + TEMPLATE_HEADERS.get(templateFieldIndex(field)) + "”.");
            }
        }
        return columns;
    }

    private static LeadRequest readLead(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        return new LeadRequest(value(row, columns, "name", formatter),
                value(row, columns, "phone", formatter),
                value(row, columns, "email", formatter),
                parseListingId(value(row, columns, "listingId", formatter)),
                value(row, columns, "source", formatter),
                value(row, columns, "subject", formatter),
                value(row, columns, "need", formatter),
                value(row, columns, "gender", formatter),
                value(row, columns, "contactChannel", formatter),
                value(row, columns, "interestLevel", formatter),
                value(row, columns, "contactStatus", formatter),
                value(row, columns, "propertyFeature", formatter),
                value(row, columns, "direction", formatter),
                value(row, columns, "position", formatter),
                value(row, columns, "interior", formatter),
                value(row, columns, "legalStatus", formatter),
                value(row, columns, "priceUnit", formatter),
                value(row, columns, "propertyStatus", formatter),
                value(row, columns, "dealStatus", formatter),
                value(row, columns, "notes", formatter),
                value(row, columns, "propertyAddress", formatter),
                coordinate(row, columns, "latitude"),
                coordinate(row, columns, "longitude"));
    }

    private static Double coordinate(Row row, Map<String, Integer> columns, String field) {
        Integer index = columns.get(field);
        if (index == null) return null;
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            double coordinate = cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                    ? cell.getNumericCellValue()
                    : Double.parseDouble(cell.getStringCellValue().trim().replace(',', '.'));
            if (!Double.isFinite(coordinate)) {
                throw new NumberFormatException("not finite");
            }
            return coordinate;
        } catch (IllegalStateException | NumberFormatException exception) {
            throw new IllegalArgumentException("Vĩ độ và kinh độ phải là số thập phân.");
        }
    }

    private void validate(LeadRequest request) {
        Set<ConstraintViolation<LeadRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) return;
        String message = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .findFirst()
                .orElse("Dữ liệu không hợp lệ.");
        throw new IllegalArgumentException(message);
    }

    private static Long parseListingId(String value) {
        if (value.isBlank()) return null;
        try {
            return new BigDecimal(value.replace(",", "")).longValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Mã tin liên quan phải là số nguyên.");
        }
    }

    private static String value(Row row, Map<String, Integer> columns, String field, DataFormatter formatter) {
        Integer index = columns.get(field);
        if (index == null) return "";
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static boolean isEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            if (!formatter.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file Excel cần nhập.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File Excel không được vượt quá 5 MB.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")
                || filename.toLowerCase(Locale.ROOT).endsWith(".xls"))) {
            throw new IllegalArgumentException("Chỉ chấp nhận file Excel có định dạng .xlsx hoặc .xls.");
        }
    }

    private static void addContactKeys(Set<String> keys, String phone, String email) {
        keys.addAll(contactKeys(phone, email));
    }

    private static Set<String> contactKeys(String phone, String email) {
        Set<String> keys = new HashSet<>();
        if (phone != null && !phone.isBlank()) {
            String normalizedPhone = phone.replaceAll("[^+0-9]", "");
            if (!normalizedPhone.isBlank()) keys.add("phone:" + normalizedPhone);
        }
        if (email != null && !email.isBlank()) keys.add("email:" + email.trim().toLowerCase(Locale.ROOT));
        return keys;
    }

    private static int templateFieldIndex(String field) {
        return switch (field) {
            case "name" -> 0;
            case "phone" -> 1;
            case "email" -> 2;
            case "listingId" -> 3;
            case "source" -> 4;
            case "subject" -> 5;
            case "need" -> 6;
            case "gender" -> 7;
            case "contactChannel" -> 8;
            case "interestLevel" -> 9;
            case "contactStatus" -> 10;
            case "propertyFeature" -> 11;
            case "direction" -> 12;
            case "position" -> 13;
            case "interior" -> 14;
            case "legalStatus" -> 15;
            case "priceUnit" -> 16;
            case "propertyStatus" -> 17;
            case "dealStatus" -> 18;
            case "notes" -> 19;
            case "propertyAddress" -> 20;
            case "latitude" -> 21;
            default -> 22;
        };
    }

    private static Map<String, String> headerFields() {
        Map<String, String> fields = new HashMap<>();
        addAliases(fields, "name", "ho ten", "ten", "ten khach hang", "name");
        addAliases(fields, "phone", "so dien thoai", "dien thoai", "phone", "mobile");
        addAliases(fields, "email", "email");
        addAliases(fields, "listingId", "ma tin lien quan", "ma tin", "listing id", "listingid");
        addAliases(fields, "source", "nguon du lieu", "nguon", "source");
        addAliases(fields, "subject", "doi tuong", "subject");
        addAliases(fields, "need", "nhu cau", "need");
        addAliases(fields, "gender", "gioi tinh", "gender");
        addAliases(fields, "contactChannel", "kenh tiep can", "kenh", "contact channel");
        addAliases(fields, "interestLevel", "muc do quan tam", "muc quan tam", "interest level");
        addAliases(fields, "contactStatus", "trang thai lien he", "contact status");
        addAliases(fields, "propertyFeature", "dac diem bds", "dac diem bat dong san", "property feature");
        addAliases(fields, "direction", "huong", "direction");
        addAliases(fields, "position", "vi tri", "position");
        addAliases(fields, "interior", "noi that", "interior");
        addAliases(fields, "legalStatus", "ho so phap ly", "phap ly", "legal status");
        addAliases(fields, "priceUnit", "don vi gia", "don vi", "price unit");
        addAliases(fields, "propertyStatus", "tinh trang bds", "tinh trang bat dong san", "property status");
        addAliases(fields, "dealStatus", "trang thai giao dich", "deal status");
        addAliases(fields, "notes", "ghi chu", "notes");
        addAliases(fields, "propertyAddress", "dia chi khu vuc bds", "dia chi bat dong san",
                "khu vuc bds", "property address");
        addAliases(fields, "latitude", "vi do", "latitude", "lat");
        addAliases(fields, "longitude", "kinh do", "longitude", "lng", "lon");
        return Map.copyOf(fields);
    }

    private static void addAliases(Map<String, String> fields, String field, String... aliases) {
        for (String alias : aliases) fields.put(normalizeHeader(alias), field);
    }

    private static String normalizeHeader(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd');
        return normalized.replaceAll("[^a-z0-9]+", " ").trim();
    }

    public record RowError(int row, String message) { }

    public record ImportResult(int imported, int duplicatesSkipped, List<RowError> errors) { }
}
