# API quản lý OCOP GO

Base URL `http://localhost:8086/api/v1/management`.

Import `docs/management.postman_collection.json` vào Postman để dùng 121 mẫu request. Điền biến môi trường/collection, đăng nhập để lưu accessToken, rồi chọn từng request cần kiểm tra.

## Chạy và cấu hình

Yêu cầu Java 21, PostgreSQL, Redis và MongoDB theo `application.yml` hiện tại.

```powershell
$env:CLOUDINARY_CLOUD_NAME = "ten-cloud"
$env:CLOUDINARY_API_KEY = "api-key"
$env:CLOUDINARY_API_SECRET = "api-secret"
mvn spring-boot:run
```

Không đưa `CLOUDINARY_API_SECRET` vào frontend. Cấu hình Cloudinary trong YAML lấy từ biến môi trường, không còn chứa khóa trực tiếp.

Hibernate tạo/cập nhật entity theo cấu hình hiện có. Sau đó `ManagementSchemaInitializer` chạy script idempotent `src/main/resources/db/migrations/management.sql`, bổ sung index và cho phép chứng nhận không có ngày hết hạn. Có thể tắt bằng `management.migrate-schema=false` khi tự quản lý migration trong môi trường triển khai.

Ba role được khởi tạo idempotent khi ứng dụng chạy. Ở profile local (mặc định khi chạy trên máy), tự tạo admin@ocop.local, partner@ocop.local và customer@ocop.local với mật khẩu Ocop@123456 nếu chưa tồn tại; không đặt lại mật khẩu ở các lần chạy sau. Profile khác không chạy seeder. Để cấp Admin đầu tiên, đăng ký tài khoản qua `/api/v1/auths/register`, rồi chạy SQL sau với email thật trong môi trường của bạn.

```sql
INSERT INTO user_roles (id,user_ref_id,role_ref_id,assigned_at,created_at,updated_at)
SELECT gen_random_uuid(),u.id,r.id,now(),now(),now()
FROM users u CROSS JOIN roles r
WHERE lower(u.email)=lower('email-admin-cua-ban@example.com') AND r.code='ADMIN'
ON CONFLICT (user_ref_id,role_ref_id) DO NOTHING;
```

Đăng nhập lại hoặc refresh token sau khi được cấp thêm role để JWT chứa quyền mới. Quyền quản lý được kiểm tra lại từ PostgreSQL trong mỗi yêu cầu. Thu hồi membership hoặc role có hiệu lực với API quản lý ngay cả khi JWT cũ chưa hết hạn. Khóa tài khoản còn thu hồi toàn bộ phiên qua phiên bản người dùng trên Redis, chặn access token và refresh token trên mọi thiết bị.

## Quy ước

- Header `Authorization: Bearer <accessToken>`.
- CUSTOMER không truy cập cổng quản lý. ADMIN toàn quyền. PARTNER chỉ truy cập cơ sở đang hoạt động mà mình là thành viên.
- Response dùng `ApiResponse<T>` hiện tại. Mọi response quản lý có `Cache-Control: no-store`.
- Phân trang `page=0`, `size=20`, tối đa 100. Sắp xếp thời gian tạo giảm dần rồi UUID.
- `Item` có `id`, `code`, `name`, `businessId`, `publicationStatus`, `rejectionReason`, `version`, `details`. `details` chứa các trường của form tương ứng.
- ID tham chiếu luôn là UUID `id`. Mã đọc được như `POI-100001` chỉ dùng hiển thị.
- Khi sửa nội dung hoặc thực hiện workflow, gửi `version` lấy từ response mới nhất. Thiếu version khi sửa hoặc version cũ trả 409; thiếu version ở body workflow trả 400. Đọc lại chi tiết sau thao tác trả `data=null`, ví dụ gắn ảnh.
- `DELETE` nội dung là lưu trữ, không xóa lịch sử. Xóa danh mục đang được tham chiếu trả 409.
- Mọi nội dung mới và nội dung vừa sửa đều thành DRAFT, cần gửi duyệt và được ADMIN duyệt lại trước khi công khai. Bản đã công khai sẽ tạm ẩn trong quá trình chỉnh sửa/duyệt lại. Bản ARCHIVED không sửa được.
- Lọc nội dung hỗ trợ `keyword`, `businessId`, `status`, `page`, `size` bằng JPA Specification. `keyword` là chuỗi chứa, có escape `%` và `_`.
- Dữ liệu POI/Product cũ được giữ trạng thái công khai hiện có. ADMIN có thể cập nhật form để gán business lần đầu cho nội dung cũ chưa có chủ sở hữu. Không hỗ trợ chuyển quyền sở hữu giữa hai business qua API sửa thông thường.

## Danh sách endpoint

### Nội dung

`{resource}` nhận `pois`, `products`, `certifications`, `stories`, `events`, `tours`.

| Method | Endpoint | Chức năng |
|---|---|---|
| GET | `/{resource}` | Danh sách trong phạm vi quyền, hỗ trợ bộ lọc chung |
| GET | `/{resource}/{id}` | Chi tiết |
| POST | `/{resource}` | Tạo bản nháp, trả 201 |
| PUT | `/{resource}/{id}` | Cập nhật đầy đủ form |
| POST | `/{resource}/{id}/submit` | DRAFT/REJECTED → PENDING_REVIEW, body `{ "version": 0 }` |
| POST | `/{resource}/{id}/approve` | ADMIN duyệt PENDING_REVIEW → PUBLISHED |
| POST | `/{resource}/{id}/reject` | ADMIN từ chối, body `{ "version": 1, "reason": "Cần bổ sung thông tin" }` |
| DELETE | `/{resource}/{id}?version=2` | Lưu trữ nội dung |
| GET | `/{resource}/{id}/media` | Ảnh/video theo thứ tự |
| PUT | `/{resource}/{id}/media` | Thay danh sách media, body `{ "version": 2, "mediaIds": ["UUID"] }` |

`mediaIds: []` gỡ toàn bộ ảnh/video khỏi nội dung. Phần tử đầu là ảnh đại diện. Ảnh/video phải READY và cùng business với nội dung. Sản phẩm dùng ảnh; địa điểm, câu chuyện, chứng nhận, tour, sự kiện hỗ trợ ảnh/video.

### Cơ sở / đối tác

| Method | Endpoint | Quyền / chức năng |
|---|---|---|
| GET | `/businesses?keyword=&page=0&size=20` | Danh sách cơ sở được phép xem |
| GET | `/businesses/{id}` | Chi tiết cơ sở |
| POST | `/businesses` | ADMIN tạo cơ sở |
| PUT | `/businesses/{id}` | Sửa hồ sơ; chỉ ADMIN đổi trạng thái hoạt động |
| GET | `/businesses/{id}/members` | Thành viên quản lý |
| PUT | `/businesses/{id}/members/{userId}` | ADMIN gán thành viên, tự bổ sung role PARTNER nếu thiếu |
| DELETE | `/businesses/{id}/members/{userId}` | ADMIN gỡ thành viên |
| GET / PUT | `/businesses/{id}/media` | Thư viện cơ sở, PUT có version của Business |

Khi gán thành viên, người được gán phải là tài khoản đang hoạt động. Người dùng có thể quản lý nhiều cơ sở; một cơ sở có nhiều người quản lý. Vai trò PARTNER không tự động cấp quyền cho mọi cơ sở.

### Danh mục

`{type}` nhận `areas`, `poi-categories`, `product-types`, `activities`, `amenities`.

| Method | Endpoint | Chức năng |
|---|---|---|
| GET | `/catalogs/{type}?keyword=&page=0&size=20` | ADMIN/PARTNER tra danh mục |
| POST | `/catalogs/{type}` | ADMIN tạo |
| PUT | `/catalogs/{type}/{id}` | ADMIN sửa |
| DELETE | `/catalogs/{type}/{id}` | ADMIN xóa nếu không có tham chiếu |

Không tạo `product_groups`. `product-types` dùng `parentId`, kiểm tra vòng lặp. `poi-categories` cũng kiểm tra vòng lặp.

### Điểm bán

| Method | Endpoint | Chức năng |
|---|---|---|
| GET | `/listings?poiId=&page=0&size=20` | Danh sách trong phạm vi quyền |
| POST | `/listings` | Tạo điểm bán sản phẩm |
| PUT | `/listings/{id}` | Cập nhật giá, tồn kho, hoạt động |
| DELETE | `/listings/{id}` | Ngừng bán |

Đối tác sở hữu địa điểm bán có thể tạo listing cho sản phẩm đã công khai hoặc sản phẩm của cơ sở mình. Không được đổi sản phẩm/địa điểm của listing đã tạo. Giá/tồn kho có hiệu lực trực tiếp, không qua duyệt.

### Tour

| Method | Endpoint | Chức năng |
|---|---|---|
| GET | `/tours/{id}/stops` | Điểm dừng có UUID và thứ tự |
| PUT | `/tours/{id}/stops` | Thay toàn bộ điểm dừng, xóa các đoạn đường cũ |
| GET | `/tours/{id}/routes` | Các đoạn đường |
| PUT | `/tours/{id}/routes` | Tạo/cập nhật đoạn nối hai điểm dừng liên tiếp |

Tọa độ route lấy từ kết quả Map4D của frontend hoặc dữ liệu đã đối chiếu. API này lưu đường đi; chưa tự gọi dịch vụ tính đường Map4D. Phải có ít nhất một điểm dừng trước khi gửi duyệt tour.

### Vận hành

| Method | Endpoint | Chức năng |
|---|---|---|
| GET | `/dashboard` | Đếm nội dung, nội dung chờ duyệt, đơn hàng, chứng nhận hết hạn trong 30 ngày; theo phạm vi quyền |
| GET | `/users?keyword=&page=0&size=20` | ADMIN tra tài khoản, không trả password hash |
| PUT | `/users/{id}/roles` | ADMIN gán tập role CUSTOMER/PARTNER/ADMIN |
| PATCH | `/users/{id}/status` | ADMIN ACTIVE/UNACTIVE |
| GET | `/orders?status=&page=0&size=20` | Đơn có hàng thuộc cơ sở mình |
| GET | `/orders/{id}` | Chi tiết, PARTNER chỉ thấy dòng hàng của mình, không nhận tổng tiền toàn đơn |
| PATCH | `/orders/{id}/items/{itemId}/fulfillment` | Chủ điểm bán xử lý dòng hàng PENDING → SHIPPED → COMPLETED sau khi đơn đã thanh toán |
| PATCH | `/orders/{id}/status` | ADMIN chuyển PAID → SHIPPED → COMPLETED |
| GET | `/payments?orderId=&status=&page=0&size=20` | ADMIN tra giao dịch |
| GET | `/reviews/{kind}?status=&page=0&size=20` | `kind=poi` hoặc `product`, gồm ảnh/video |
| PATCH | `/reviews/{kind}/{id}` | ADMIN PUBLISHED/HIDDEN, bắt buộc lý do |
| GET | `/audits?targetType=&targetId=&page=0&size=20` | ADMIN xem lịch sử thay đổi, người thực hiện |

Không cho tự thu hồi quyền ADMIN của chính mình hoặc khóa tài khoản đang dùng. Giữ ít nhất một ADMIN đang hoạt động. Thao tác phân quyền quản trị được tuần tự hóa trong PostgreSQL.

Đánh giá địa điểm bị ẩn/hiện sẽ được tính lại điểm trung bình và số lượt đánh giá. Đối tác không tự kiểm duyệt đánh giá về mình.

**Thanh toán** hiện có API quản lý/tra cứu dữ liệu payment. Tạo payment link payOS, checkout, webhook xác thực thanh toán, hoàn tiền và hủy đơn có hoàn tồn kho chưa thuộc bộ API quản lý này. Không có endpoint sửa PAID thủ công; dữ liệu thanh toán thực tế cần luồng xác nhận payOS riêng.

## Form request

Các tên dưới đây dùng camelCase. Trường không áp dụng có thể bỏ qua; các trường bắt buộc theo record và Bean Validation trong `ManagementRequest.java`.

### BusinessForm

```json
{
  "name": "Cơ sở OCOP",
  "introduction": "Giới thiệu cơ sở",
  "representative": "Người đại diện",
  "phone": "0900000000",
  "email": "contact@example.com",
  "website": "https://example.com",
  "socialLinks": {"facebook": "https://www.facebook.com/example"},
  "active": true,
  "version": 0
}
```

`version` bỏ qua khi tạo. Các chuỗi liên hệ chưa có dùng `""`, socialLinks dùng `{}`. Website và liên kết mạng xã hội chỉ nhận HTTP/HTTPS.

### PoiForm

```json
{
  "businessId": "UUID-business",
  "areaId": "UUID-area",
  "name": "Cơ sở nước mắm",
  "address": "Đà Nẵng",
  "lat": 16.1,
  "lng": 108.1,
  "openingHours": {"monday": [{"open": "08:00", "close": "17:00"}]},
  "categoryIds": ["UUID-category"],
  "activityIds": [],
  "amenityIds": [],
  "version": 0
}
```

Có thể bỏ cả lat/lng khi chưa xác minh vị trí. Khi gửi một tọa độ phải có cả hai. Giờ HH:mm, không chồng nhau, đóng cửa sau mở cửa trong cùng ngày; ngày không mở dùng `[]` hoặc bỏ ngày đó. Nội dung chưa có tọa độ vẫn lưu được nhưng không xuất hiện trên bản đồ.

### ProductForm

```json
{"manufacturerPoiId":"UUID-poi","productTypeId":"UUID-type","name":"Tên sản phẩm","description":"Mô tả","version":0}
```

Cơ sở sở hữu suy ra từ địa điểm sản xuất. Không nhận businessId tự do.

### ListingForm

```json
{"poiId":"UUID-poi","productId":"UUID-product","priceVnd":150000,"stockQuantity":25,"active":true}
```

Giá VND là số nguyên dương, tồn kho nguyên không âm. Cặp sản phẩm/địa điểm chỉ xuất hiện một lần.

### CertificateForm

```json
{"productId":"UUID-product","type":"OCOP","ocopStars":4,"name":"Chứng nhận OCOP","certificateNumber":"Số chứng nhận","issuingAuthority":"Cơ quan cấp","issuedOn":"2025-01-01","expiresOn":"2028-01-01","version":0}
```

Chọn đúng một trong productId/poiId. OCOP chỉ cho sản phẩm và phải có 3–5 sao. OTHER không có ocopStars. Chứng nhận địa điểm dùng OTHER. expiresOn có thể bỏ qua nếu không có thời hạn. Đối tượng, loại và số sao cố định sau khi tạo; cần tạo chứng nhận mới nếu cấp lại mức sao khác.

### StoryForm

```json
{"poiId":"UUID-poi","name":"Câu chuyện thương hiệu","slug":"cau-chuyen-thuong-hieu","introduction":"Giới thiệu","founder":"Tên người sáng lập","foundedYear":1945,"sections":[{"heading":"Quy trình sản xuất","body":"Nội dung dạng văn bản","mediaIds":["UUID-media"]}],"version":0}
```

Nội dung là văn bản. Frontend không render body thành HTML chưa làm sạch. Media nhúng trong section được ghi nhận liên kết để chặn xóa nhầm. Dữ liệu câu chuyện dùng JSONB trong PostgreSQL để giao dịch và quyền sở hữu nhất quán với POI; chưa phát sinh bản sao MongoDB.

### EventForm

```json
{"poiId":"UUID-poi","name":"Sự kiện OCOP","slug":"su-kien-ocop","description":"Nội dung","startsAt":"2026-12-01T01:00:00Z","endsAt":"2026-12-01T10:00:00Z","organizer":"Đơn vị tổ chức","registrationUrl":"","recurrence":"","version":0}
```

Thời gian ISO-8601 có múi giờ, endsAt sau startsAt. recurrence là mô tả lịch lặp, chưa tự sinh các kỳ sự kiện.

### TourForm / StopsForm / RouteForm

```json
{"businessId":"UUID-business","name":"Tour trải nghiệm OCOP","slug":"tour-trai-nghiem-ocop","description":"Mô tả tour","version":0}
```

```json
{"version":0,"stops":[{"poiId":"UUID-poi-1","visitDurationMinutes":30},{"poiId":"UUID-poi-2","visitDurationMinutes":45}]}
```

```json
{"version":1,"fromStopId":"UUID-stop-1","toStopId":"UUID-stop-2","travelMode":"DRIVING","pathCoordinates":[{"lat":16.05,"lng":108.2},{"lat":16.06,"lng":108.21}],"distanceMeters":1500,"durationSeconds":300}
```

stops tối đa 100. pathCoordinates từ 2 đến 10.000 điểm. travelMode WALKING/DRIVING. Khoảng cách và thời gian không âm. Thay điểm dừng sẽ thay UUID stop và gỡ đường đi cũ.

### CatalogForm

```json
{"name":"Thực phẩm","code":"FOOD","slug":"thuc-pham","iconUrl":"/icons/food.svg","colorHex":"#24823C","parentId":null,"sortOrder":0,"active":true}
```

- areas cần name/code/active.
- activities và amenities thêm iconUrl/sortOrder.
- product-types cần name/slug/iconUrl/sortOrder/active, parentId tùy chọn.
- poi-categories dùng name/iconUrl/colorHex/parentId. colorHex dạng #RRGGBB.
- code danh mục viết hoa, bắt đầu bằng chữ cái; slug chữ thường và dấu gạch nối.

### Tài khoản / vận hành

```json
{"roles":["CUSTOMER","PARTNER"]}
```

```json
{"status":"UNACTIVE"}
```

```json
{"status":"SHIPPED"}
```

```json
{"status":"HIDDEN","reason":"Nội dung không phù hợp"}
```

## Cloudinary

1. POST `/media/upload-signature` với `{ "businessId": "UUID", "resourceType": "image", "altText": "Mô tả ảnh" }`. resourceType là image/video.
2. Nhận UploadTicket có id nội bộ, uploadUrl, apiKey, publicId, timestamp, signature, expiresAt.
3. Frontend gửi multipart trực tiếp tới uploadUrl, gồm `file`, `api_key`, `public_id`, `timestamp`, `signature`, **`overwrite=false`**. Các giá trị phải đúng ticket; không tự đổi public_id hoặc bổ sung tham số có chữ ký.
4. POST `/media/{id}/complete`. Backend gọi Cloudinary để đọc metadata thật, không nhận secureUrl/dung lượng do frontend tự khai.
5. PUT media vào đối tượng quản lý như mô tả ở trên. Có thể chọn lại tài nguyên đã tải, không phải upload lại.

Các endpoint bổ sung gồm GET `/media?businessId=&keyword=&resourceType=&page=0&size=20`, GET `/media/{id}/usage`, PATCH `/media/{id}` với `{ "altText": "Mô tả" }`, DELETE `/media/{id}`.

Giới hạn xác nhận hiện tại ảnh 10 MiB, video 200 MiB. Ảnh nhận jpg/jpeg/png/webp/avif/gif; video nhận mp4/mov/webm. Ticket xác nhận trong 15 phút. Cấu hình giới hạn upload tương ứng trên Cloudinary cho môi trường triển khai để chặn tệp quá lớn ngay tại nơi nhận file.

Xóa tài nguyên đang có liên kết trả 409. Gỡ liên kết không xóa file. Xóa file dùng public_id/resource_type và yêu cầu Cloudinary invalidate. Database giữ metadata ở trạng thái DELETED để có lịch sử. Các thao tác attach/delete khóa cùng asset để tránh xóa tài nguyên vừa được gắn ở giao dịch khác.

Kiểm thử tự động dùng CloudinaryGateway giả lập, không tải tài nguyên thật và không tiêu quota tài khoản. Cần kiểm tra upload trực tiếp với cấu hình tài khoản Cloudinary thực tế khi tích hợp frontend.

## Nhập Excel

Chỉ ADMIN, mỗi batch chỉ người tạo được sửa/xác nhận. Không tự nhập DATASET.xlsx hoặc thư mục RESOURCES vào database.

POST `/imports/{resource}/preview` multipart, các part sau.

- file là Excel .xlsx/.xls, tối đa 5 MB.
- mapping là JSON với tên thuộc tính form → tên cột Excel, ví dụ `{ "name": "Tên cơ sở", "phone": "Số điện thoại" }`.
- defaults là JSON tùy chọn, cung cấp giá trị chung như businessId/areaId, các danh sách rỗng.
- Query sheetName chọn sheet; headerRow là dòng tiêu đề tính từ 1, mặc định 1.

Ví dụ nhập POI từ cột tên/địa chỉ, không đoán tọa độ hoặc tự gán sao OCOP.

```json
{"name":"Tên cơ sở","address":"Địa chỉ"}
```

```json
{"businessId":"UUID-business","areaId":"UUID-area","openingHours":{},"categoryIds":[],"activityIds":[],"amenityIds":[]}
```

Danh sách/đối tượng trong ô phải là JSON hợp lệ. Không chạy ô công thức. File tối đa 500 dòng dữ liệu, 100 cột; parser giữ giới hạn chống ZIP bomb.

Preview trả từng row, dữ liệu, errors và warnings. `row` đánh số thứ tự các dòng dữ liệu không rỗng trong bản xem trước. Trùng tên trong batch bị báo lỗi để đối chiếu; tên trùng với nội dung hiện có cũng bị chặn trong preview. Không tự gộp cơ sở cùng tên. Các ràng buộc quan hệ/nghiệp vụ tiếp tục được kiểm tra khi commit; lỗi khiến toàn bộ batch rollback.

- GET `/imports/{id}` xem lại.
- PUT `/imports/{id}/rows` với `{ "version": 0, "rows": [ { "name": "Tên đã sửa", "...": "đầy đủ thuộc tính form" } ] }` thay dữ liệu sau chỉnh sửa.
- POST `/imports/{id}/commit` với `{ "version": 1 }` để xác nhận. Chỉ commit khi không có lỗi preview. Nội dung nhập vào là DRAFT.
- Batch hết hạn sau 24 giờ. Gửi lại commit đã thành công trả cùng importedIds, không tạo lại dữ liệu.

Resource nhận businesses/pois/products/listings/certifications/stories/events/tours. Nhập theo thứ tự tham chiếu, lấy UUID của cơ sở, danh mục, POI và sản phẩm đã có; không tự nối các mã cũ không xác định trong Excel. Điện thoại nên là cột Text để giữ số 0 đầu.

## Hiệu năng và lỗi

Danh sách dùng phân trang tại SQL; dữ liệu liên quan dùng LAZY/BatchSize, không join-fetch nhiều collection cùng truy vấn phân trang. Có index ownership/status/thời gian và tham chiếu media. Redis home cache đổi generation sau commit, không dùng KEYS để xóa hàng loạt. Nếu Redis lỗi khi làm mới cache, cache cũ hết hạn theo TTL hiện có.

HTTP 400 dữ liệu không hợp lệ, 401 chưa đăng nhập/token hết hiệu lực, 403 thiếu quyền, 404 không có dữ liệu, 409 trùng/ràng buộc/version cũ/tài nguyên đang sử dụng, 503 dịch vụ dữ liệu hoặc Cloudinary gián đoạn. Thông báo và Bean Validation mặc định bằng tiếng Việt.

Chạy `mvn -o test` trong môi trường có PostgreSQL/Redis/MongoDB hiện tại. Bộ kiểm tra gồm xác thực, home và API quản lý qua HTTP thật; Cloudinary được giả lập riêng.


## Upload biểu tượng danh mục và ảnh/video trong popup

- ADMIN: `POST /api/v1/management/media/catalog-images/upload-signature` (không cần body/cơ sở) cấp ticket upload ảnh biểu tượng. Frontend gửi file trực tiếp lên Cloudinary với chữ ký, không gửi access token của ứng dụng sang Cloudinary.
- ADMIN: `POST /api/v1/management/media/catalog-images/complete`, body `{ "publicId": "ocop/catalog-icons/UUID" }`. Backend kiểm tra lại tài nguyên Cloudinary, trả `id`, `secureUrl`; frontend tự đưa URL vào `iconUrl` của form. Người dùng chỉ chọn tệp, không nhập URL. Ảnh tối đa 10 MB, hỗ trợ JPG/PNG/WEBP/AVIF/GIF.
- Biểu tượng dùng chung cho danh mục nên không buộc gắn cơ sở. Biểu tượng chưa lưu vào danh mục hoặc bị thay thế vẫn tồn tại trên Cloudinary; chưa có tác vụ tự dọn ảnh biểu tượng không còn dùng.
- Popup sửa địa điểm, sản phẩm, chứng nhận, câu chuyện, sự kiện, tour và cơ sở có bộ chọn tệp trực tiếp. Tài nguyên nội dung vẫn dùng ticket có `businessId` và bước complete hiện có, kiểm tra quyền sở hữu như trước.
- Tải tệp xong, chọn **Lưu danh sách ảnh/video** để gắn vào nội dung. Danh sách giữ các ảnh đã có, phần tử đầu là ảnh đại diện. Nội dung mới cần lưu trước để có ID; sau đó mở Xem / sửa để gắn ảnh/video. Sản phẩm chỉ nhận ảnh; nội dung khác nhận ảnh và video.
- Giới hạn nội dung: ảnh 10 MB, video 200 MB; tối đa 50 tài nguyên trên một nội dung. Backend API secret chỉ cấu hình qua môi trường backend.
