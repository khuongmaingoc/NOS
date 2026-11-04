package vn.sonbds.marketplace.realestate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LeadExcelImportServiceTest {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private final Validator validator = VALIDATOR_FACTORY.getValidator();
    private CustomerLeadRepository leads;
    private PropertyListingRepository listings;
    private LeadExcelImportService importer;

    @BeforeEach
    void setUp() {
        leads = mock(CustomerLeadRepository.class);
        listings = mock(PropertyListingRepository.class);
        importer = new LeadExcelImportService(leads, listings, validator);
        when(leads.findByOwnerIdOrderByUpdatedAtDesc(17L)).thenReturn(List.of(
                new CustomerLead(17L, null, request("Khách cũ", "0901 234 567", "old@example.com"))));
        when(listings.findByOwnerIdOrderByCreatedAtDesc(17L)).thenReturn(List.of());
    }

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void importsValidRowsSkipsDuplicatesAndReportsInvalidRows() throws Exception {
        MockMultipartFile file = excelFile("customers.xlsx", workbook -> {
            Row header = workbook.createSheet().createRow(0);
            addHeaders(header);
            header.createCell(20).setCellValue("Địa chỉ/khu vực BĐS");
            header.createCell(21).setCellValue("Vĩ độ");
            header.createCell(22).setCellValue("Kinh độ");
            Row duplicate = workbook.getSheetAt(0).createRow(1);
            duplicate.createCell(0).setCellValue("Trùng khách");
            duplicate.createCell(1).setCellValue("0901234567");
            duplicate.createCell(2).setCellValue("different@example.com");

            Row valid = workbook.getSheetAt(0).createRow(2);
            valid.createCell(0).setCellValue("Khách mới");
            valid.createCell(1).setCellValue("0909 888 777");
            valid.createCell(2).setCellValue("new@example.com");
            valid.createCell(20).setCellValue("Quận 1, TP.HCM");
            valid.createCell(21).setCellValue(10.7769);
            valid.createCell(22).setCellValue(106.7009);

            Row invalid = workbook.getSheetAt(0).createRow(3);
            invalid.createCell(0).setCellValue("Email sai");
            invalid.createCell(2).setCellValue("not-an-email");

            Row incompleteCoordinates = workbook.getSheetAt(0).createRow(4);
            incompleteCoordinates.createCell(0).setCellValue("Thiếu kinh độ");
            incompleteCoordinates.createCell(21).setCellValue(10.8);
        });

        LeadExcelImportService.ImportResult result = importer.importFile(17L, file);

        assertEquals(1, result.imported());
        assertEquals(1, result.duplicatesSkipped());
        assertEquals(2, result.errors().size());
        assertEquals(4, result.errors().get(0).row());
        assertEquals(5, result.errors().get(1).row());
        verify(leads).saveAll(any());
    }

    @Test
    void rejectsWorkbookWithoutRequiredNameColumn() throws Exception {
        MockMultipartFile file = excelFile("customers.xlsx", workbook -> {
            Row header = workbook.createSheet().createRow(0);
            header.createCell(0).setCellValue("Điện thoại");
        });

        assertThrows(IllegalArgumentException.class, () -> importer.importFile(17L, file));
        verifyNoInteractions(leads, listings);
    }

    @Test
    void rejectsUnsupportedFileExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", "Họ tên\nKhách".getBytes());

        assertThrows(IllegalArgumentException.class, () -> importer.importFile(17L, file));
        verifyNoInteractions(leads, listings);
    }

    private static MockMultipartFile excelFile(String name, WorkbookWriter writer) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writer.write(workbook);
            workbook.write(output);
            return new MockMultipartFile("file", name,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private static void addHeaders(Row row) {
        row.createCell(0).setCellValue("Họ tên");
        row.createCell(1).setCellValue("Số điện thoại");
        row.createCell(2).setCellValue("Email");
    }

    private static LeadRequest request(String name, String phone, String email) {
        return new LeadRequest(name, phone, email, null, "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "");
    }

    @FunctionalInterface
    private interface WorkbookWriter {
        void write(XSSFWorkbook workbook) throws Exception;
    }
}
