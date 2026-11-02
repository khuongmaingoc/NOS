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
- Chủ sở hữu cấp/thu hồi quyền `VIEW`, `EDIT`, `DELETE` riêng theo từng tài khoản và từng tin/dự án. Người có quyền sửa hoặc xóa cũng được xem tài nguyên.
- Đầu khách lưu tin, gửi yêu cầu liên hệ; đầu chủ cập nhật trạng thái tin (`ACTIVE`, `RESERVED`, `SOLD`, `RENTED`) và trạng thái liên hệ (`NEW`, `RESPONDED`, `CLOSED`).
- Phân quyền REST API được kiểm tra ở tầng ứng dụng trên từng tài nguyên; không dựa vào việc ẩn nút trên giao diện.
- `spring.jpa.hibernate.ddl-auto=update` chỉ thuận tiện cho phát triển local. Trước khi đưa vào production, cấu hình migration (Flyway/Liquibase), secrets, HTTPS, backup và chính sách quản trị tài khoản.

## REST API

API dùng session đăng nhập từ website. Với thao tác thay đổi dữ liệu, lấy token tại `GET /api/csrf`, sau đó gửi CSRF token ở header (tên header được trả về) cùng cookie session.

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/register` | Đăng ký, JSON gồm `name`, `email`, `password`, `role` |
| GET | `/api/listings` | Tin công khai/tin được cấp quyền xem; hỗ trợ `?transactionType=Bán` |
| GET, POST | `/api/listings`, `/api/listings/{id}` | Xem tin, tạo tin (SELLER), xem chi tiết |
| PUT, PATCH, DELETE | `/api/listings/{id}` | Sửa, cập nhật trạng thái (`/status`), xóa theo quyền |
| GET, POST, PUT, DELETE | `/api/projects`, `/api/projects/{id}` | Dự án phân lô và quản lý theo quyền |
| POST, GET, DELETE | `/api/access-grants` | Chủ sở hữu cấp, liệt kê, thu hồi quyền |
| POST, DELETE | `/api/listings/{id}/favorites` | Lưu/bỏ lưu tin (CUSTOMER) |
| POST | `/api/listings/{id}/inquiries` | Đầu khách gửi yêu cầu liên hệ |
| GET | `/api/my/inquiries`, `/api/seller/inquiries` | Danh sách yêu cầu liên hệ |
| PATCH | `/api/inquiries/{id}/status` | Người bán cập nhật trạng thái liên hệ |

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
  "description": "Nhà 2 tầng, pháp lý rõ ràng.",
  "price": 5200000000,
  "area": 80,
  "transactionType": "Bán",
  "propertyType": "Nhà phố",
  "visibility": "PUBLIC",
  "imageUrls": ["https://example.com/house.jpg"]
}
```

## Kiểm tra

Chạy bộ kiểm tra: `mvn test`. Build: `mvn package`.
