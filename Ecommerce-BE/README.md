# Web Xem Phim Online

## Giới thiệu
Đây là một ứng dụng web xem phim trực tuyến được xây dựng bằng Spring Boot và Thymeleaf. Ứng dụng cho phép người dùng xem phim, bình luận và tương tác với nội dung.

## Tính năng chính
- Xem phim trực tuyến với nhiều phần
- Hệ thống bình luận
- Danh sách phim đề xuất
- Giao diện người dùng thân thiện
- Hỗ trợ nhiều định dạng video
- Tính năng chia sẻ phim

## Công nghệ sử dụng
- **Backend**: Spring Boot
- **Frontend**: 
  - Thymeleaf
  - Bootstrap 5
  - jQuery
  - Font Awesome
- **Database**: MySQL
- **Video Player**: HTML5 Video Player

## Cài đặt và Chạy

### Yêu cầu hệ thống
- Java JDK 8 trở lên
- Maven
- MySQL

### Các bước cài đặt
1. Clone repository:
```bash
git clone [repository-url]
```

2. Cấu hình database trong `application.properties`

3. Build project:
```bash
mvn clean install
```

4. Chạy ứng dụng:
```bash
mvn spring-boot:run
```

## Cấu trúc thư mục
```
src/
├── main/
│   ├── java/
│   │   └── com/example/
│   │       ├── controllers/
│   │       ├── models/
│   │       ├── repositories/
│   │       └── services/
│   └── resources/
│       ├── static/
│       │   ├── css/
│       │   ├── js/
│       │   └── images/
│       └── templates/
│           ├── fragments/
│           └── pages/
└── test/
```

## Tính năng chi tiết

### Xem phim
- Phát video theo phần
- Tự động chuyển phần tiếp theo
- Hiển thị trạng thái đang tải
- Danh sách các phần video

### Bình luận
- Thêm bình luận mới
- Xóa bình luận
- Hiển thị thời gian bình luận
- Avatar người dùng

### Giao diện
- Thiết kế responsive
- Dark mode
- Thanh điều hướng dễ sử dụng
- Hiển thị thông tin phim chi tiết


