# Sơn BĐS — Chợ bất động sản

Ứng dụng web Spring Boot + Thymeleaf, REST API, Spring Security, Spring Data JPA/Hibernate và MySQL. Java 21, Spring Boot 3.5.

## Chạy local

Yêu cầu JDK 21 trở lên, Maven 3.9+ và Docker Desktop (hoặc MySQL 8.4).

1. Khởi chạy MySQL: `docker compose up -d mysql`
2. Chạy ứng dụng: `mvn spring-boot:run`
3. Mở `http://localhost:8080`, tạo tài khoản đầu chủ hoặc đầu khách.

Mặc định MySQL local dùng database `sonbds`, user `root`, mật khẩu `localdev`. Có thể cấu hình qua `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; mật khẩu Docker đổi bằng biến `MYSQL_ROOT_PASSWORD` (khi khởi tạo volume lần đầu). Dữ liệu MySQL được giữ trong volume `sonbds_mysql_data`.

## Quyền và an toàn

- Mật khẩu được băm bằng BCrypt; đăng nhập sử dụng session cookie, CSRF được bật mặc định.
- Đăng ký chỉ cho phép vai trò `SELLER` hoặc `CUSTOMER`; không thể tự đăng ký `ADMIN`.
- Chủ tin/dự án được xem, sửa, xóa tài nguyên của mình. Tin/dự án `PUBLIC` cho phép xem công khai; tài nguyên `PRIVATE` chỉ chủ sở hữu hoặc người được cấp quyền xem.
- Chủ sở hữu cấp/thu hồi quyền `VIEW`, `EDIT`, `DELETE` riêng theo từng tài khoản và từng tin/dự án. Người có quyền sửa hoặc xóa cũng được xem tài nguyên. ADMIN có thể cấp/thu hồi riêng quyền `VIEW` cho đầu khách theo từng tin tại `/admin/permissions`; quyền này không cho phép sửa hoặc xóa.
- Đầu chủ có thể mở **Quản lý của tôi → Sửa tin** để cập nhật tin và toàn bộ thuộc tính BĐS; quyền `EDIT` trên tài nguyên được kiểm tra cả khi mở form lẫn khi lưu.
- Người xem có thể mở trang chi tiết từng tin từ nút **Xem chi tiết** trên thẻ tin; trang hiển thị hình ảnh, mô tả, giá, diện tích và thông tin bất động sản, đồng thời tôn trọng quyền xem công khai/riêng tư.
- Đầu khách lưu tin, gửi yêu cầu liên hệ; đầu chủ cập nhật trạng thái tin (`ACTIVE`, `RESERVED`, `SOLD`, `RENTED`) và trạng thái liên hệ (`NEW`, `RESPONDED`, `CLOSED`).
- Đầu chủ có CRM khách hàng riêng tại `/leads`: thêm, sửa, xóa, theo dõi nguồn khách, nhu cầu, lịch sử chăm sóc, thông tin liên lạc và liên kết tin của mình. Có thể tải file mẫu tại `/leads/import-template`, nhập `.xlsx` hoặc `.xls` tối đa 5 MB; hồ sơ trùng số điện thoại/email được bỏ qua, lỗi được báo theo số dòng và dữ liệu hợp lệ vẫn nhập. Nên định dạng cột số điện thoại là Văn bản để giữ số 0 đầu. Chỉ liên kết được tin thuộc tài khoản hiện tại. Trường phân loại dùng danh sách gợi ý nhưng cho phép nhập giá trị tùy chỉnh.
- Tin đăng lưu thêm đặc điểm BĐS, hướng, vị trí, nội thất, hồ sơ pháp lý và đơn vị giá. Các lựa chọn gợi ý được trích từ bảng danh mục người dùng cung cấp.
- Giá tin đăng được chuẩn hóa thành số nguyên, làm tròn half-up đến bội số 1.000 cho mọi đơn vị giá; giá sau làm tròn phải lớn hơn 0.
- Tài khoản `ADMIN` truy cập dashboard tại `/admin`, xem tài khoản đầu chủ/đầu khách, thông tin liên hệ và CRM, số lượng tin/lead, thống kê giá và diện tích. Menu trái có mục **Gán quyền xem tin** để chọn tin của đầu chủ và đầu khách được xem; quyền có thể thu hồi trong danh sách trên cùng màn hình. Dashboard cũng có thể lọc khoảng ngày, khu vực, hình thức, loại BĐS, trạng thái, khoảng giá/diện tích và đơn vị giá; bảng phân tích nhóm theo hình thức, loại, trạng thái, hướng, khu vực, tháng, khoảng giá hoặc diện tích. Khi lọc khoảng giá phải chọn một đơn vị tiền tệ.
- Form tin đăng và CRM cho phép ghim điểm trên bản đồ OpenStreetMap/Leaflet, lưu vĩ độ/kinh độ để sửa và xem lại. Địa chỉ/khu vực được người dùng nhập tay; ứng dụng không gửi tọa độ tới dịch vụ reverse-geocoding. Ô bản đồ vẫn tải tile từ OpenStreetMap và nhà cung cấp có thể nhận địa chỉ IP cùng vùng bản đồ đang xem; xem [chính sách tile của OpenStreetMap](https://operations.osmfoundation.org/policies/tiles/) và [bản quyền OpenStreetMap](https://www.openstreetmap.org/copyright). CRM Excel hỗ trợ thêm cột `Địa chỉ/khu vực BĐS`, `Vĩ độ`, `Kinh độ`.
- Phân quyền REST API được kiểm tra ở tầng ứng dụng trên từng tài nguyên; không dựa vào việc ẩn nút trên giao diện.
- `spring.jpa.hibernate.ddl-auto=update` chỉ thuận tiện cho phát triển local. Trước khi đưa vào production, cấu hình migration (Flyway/Liquibase), secrets, HTTPS, backup và chính sách quản trị tài khoản.

## REST API

API dùng session đăng nhập từ website. Với thao tác thay đổi dữ liệu, lấy token tại `GET /api/csrf`, sau đó gửi CSRF token ở header (tên header được trả về) cùng cookie session.

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/register` | Đăng ký, JSON gồm `name`, `email`, `password`, tùy chọn `phone`, `role` |
| GET | `/api/listings` | Tin công khai/tin được cấp quyền xem; hỗ trợ `?transactionType=Bán` |
| GET, POST | `/api/listings`, `/api/listings/{id}` | Xem tin, tạo tin (SELLER), xem chi tiết |
| PUT, PATCH, DELETE | `/api/listings/{id}` | Sửa, cập nhật trạng thái (`/status`), xóa theo quyền |
| GET, POST, PUT, DELETE | `/api/projects`, `/api/projects/{id}` | Dự án phân lô và quản lý theo quyền |
| POST, GET, DELETE | `/api/access-grants` | Chủ sở hữu cấp, liệt kê, thu hồi quyền |
| POST, DELETE | `/api/listings/{id}/favorites` | Lưu/bỏ lưu tin (CUSTOMER) |
| POST | `/api/listings/{id}/inquiries` | Đầu khách gửi yêu cầu liên hệ, có thể kèm tên, điện thoại, nguồn, đối tượng, nhu cầu, giới tính, kênh, mức quan tâm, trạng thái liên hệ/giao dịch |
| GET | `/api/my/inquiries`, `/api/seller/inquiries` | Danh sách yêu cầu liên hệ |
| PATCH | `/api/inquiries/{id}/status` | Người bán cập nhật trạng thái liên hệ |
| GET, POST | `/api/leads` | Danh sách CRM của người bán, tạo khách hàng tiềm năng |
| GET, PUT, DELETE | `/api/leads/{id}` | Xem, sửa, xóa khách hàng thuộc tài khoản (hoặc ADMIN) |
| GET | `/api/admin/dashboard` | Dashboard thống kê (ADMIN), hỗ trợ các query filter như trang `/admin` |

Tin đăng API nhận thêm các trường tùy chọn `latitude` và `longitude` (phải gửi cùng nhau; vĩ độ từ -90 đến 90, kinh độ từ -180 đến 180). CRM nhận thêm `propertyAddress`, `latitude`, `longitude`. Tọa độ được lưu trong MySQL và trả về cùng bản ghi của chủ sở hữu/quyền xem tương ứng.

### Cấp quyền ADMIN

Để tránh người dùng tự nâng quyền, ứng dụng không có đăng ký công khai vai trò ADMIN. Tạo tài khoản bình thường, sau đó quản trị viên database cấp quyền một lần (thay email bằng tài khoản cần cấp):

```sql
INSERT INTO account_roles (account_id, roles)
SELECT id, 'ADMIN' FROM accounts WHERE email = 'admin@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM account_roles WHERE account_id = accounts.id AND roles = 'ADMIN'
  );
```

Đăng xuất rồi đăng nhập lại để phiên mới nhận vai trò ADMIN. Số điện thoại khai báo khi đăng ký được quản trị viên xem cùng tên/email trên dashboard. Dữ liệu liên hệ CRM cũng chỉ được hiển thị trong khu vực ADMIN hoặc cho đầu chủ sở hữu CRM.

Giao diện CRM: đăng nhập bằng tài khoản `SELLER`, mở **Quản lý của tôi → Quản lý khách hàng** hoặc vào `/leads`. Danh mục gợi ý trong form có thể nhập giá trị khác nếu nghiệp vụ cần.

Ví dụ cấp quyền sửa tin `id=12` cho tài khoản `id=8`:

```json
{
  "resourceType": "LISTING",
  "resourceId": 12,
  "accountId": 8,
  "permission": "EDIT"
}
```

Tạo tin nhận bán:

```json
{
  "title": "Nhà phố gần trung tâm",
  "address": "Quận 7, TP. Hồ Chí Minh",
  "latitude": 10.7291,
  "longitude": 106.7197,
  "description": "Nhà 2 tầng, pháp lý rõ ràng.",
  "price": 5200000000,
  "area": 80,
  "transactionType": "Bán",
  "propertyType": "Nhà phố",
  "propertyFeature": "2PN",
  "direction": "Đông Nam",
  "position": "Góc",
  "interior": "Cơ bản",
  "legalStatus": "Sở hữu lâu dài",
  "priceUnit": "VND",
  "propertyStatus": "Sẵn sàng",
  "visibility": "PUBLIC",
  "imageUrls": ["https://example.com/house.jpg"]
}
```

Tạo khách hàng CRM:

```json
{
  "name": "Nguyễn Văn A",
  "phone": "0900000000",
  "email": "a@example.com",
  "listingId": 12,
  "source": "Fanpage",
  "subject": "Chủ sở hữu",
  "need": "Cho thuê",
  "gender": "Nam",
  "contactChannel": "Zalo",
  "interestLevel": "Đang cân nhắc",
  "contactStatus": "Cần gọi lại",
  "propertyFeature": "2PN",
  "direction": "Đông Nam",
  "position": "Góc",
  "interior": "Full nội thất",
  "legalStatus": "Sở hữu lâu dài",
  "priceUnit": "VND",
  "propertyStatus": "Sẵn sàng",
  "dealStatus": "Thành công",
  "notes": "Gọi lại buổi chiều"
}
```

## Kiểm tra

Chạy bộ kiểm tra: `mvn test`. Build: `mvn package`.
