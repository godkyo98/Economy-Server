# 💰 Kyo Economy 

![Minecraft Version](https://img.shields.io/badge/Minecraft-26.2-2ea44f?style=for-the-badge&logo=minecraft)
![Platform](https://img.shields.io/badge/Fabric-Server--Side-E3C95A?style=for-the-badge)
![API](https://img.shields.io/badge/Polymer-UI-blue?style=for-the-badge)

**Kyo Economy** là một hệ thống Kinh tế và Giao thương toàn diện, được thiết kế độc quyền cho **TEA Server**. Chạy hoàn toàn trên máy chủ (100% Server-side), người chơi không cần cài đặt thêm bất kỳ mod nào ở Client mà vẫn được trải nghiệm hệ thống Cửa hàng, Chợ đen và Thẩm định vật phẩm cực kỳ trực quan.

---

## ✨ Tính Năng Nổi Bật

* 🛡️ **Kiến Trúc Dữ Liệu Tối Tân**: Loại bỏ hoàn toàn hệ thống NBT cũ kỹ dễ gây lỗi. Kyo Economy sử dụng công nghệ `DataFixerUpper (DFU)` kết hợp `RecordCodecBuilder` mới nhất của Mojang để đọc/ghi dữ liệu siêu tốc, chống thất thoát tài sản tuyệt đối.
* ⚡ **100% Server-Side**: Sử dụng thư viện **Polymer** và **SGUI**, mang đến giao diện túi đồ tùy chỉnh mượt mà mà không bắt ép người chơi tải mod.
* 🏦 **Hệ Thống Tiền Tệ Kép**: Sử dụng hệ quy chiếu thông minh `1 Xu = 1000 Hào` giúp nền kinh tế linh hoạt từ những giao dịch siêu nhỏ đến các phi vụ bạc tỷ.
* 🛒 **Cửa Hàng Admin & Chợ Đen (AH)**: 
  * Giao thương nhu yếu phẩm cơ bản thông qua `/shop`.
  * Giao dịch tự do, đấu giá vật phẩm giữa các người chơi thông qua `/ah`.
* 🔍 **Máy Thẩm Định Giá Thông Minh**: Cơ chế độc quyền cho phép người chơi `Chuột Phải` vào trang bị phù phép để **Thẩm định giá** (báo cáo chi tiết số tiền thu được), và `Shift + Chuột Phải` để **Bán đứt** nhanh gọn. Giá trị phù phép được chia làm 4 Bậc từ Cơ bản đến Thần cấp!
* 📊 **Tối Ưu Hiển Thị Scoreboard**: Tự động rút gọn tiền tệ khổng lồ thành định dạng chuyên nghiệp (`15.5k`, `1.2M`, `4.5B`) qua PlaceholderAPI, chống vỡ khung Scoreboard & TabList.

---

## 📦 Yêu Cầu Cài Đặt (Dependencies)

Để Kyo Economy hoạt động hoàn hảo, máy chủ của bạn cần cài đặt các mod lõi sau:
* **Fabric API** (Phiên bản tương thích 26.2)
* **Polymer** (Core & Resource Pack)
* **SGUI** (Server-side GUIs)
* **Placeholder API** (eu.pb4.placeholder-api v3+)

---

## 💻 Danh Sách Lệnh & Quyền Hạn (Commands)

| Lệnh mặc định | Chức năng | Quyền hạn (Permission) |
| :--- | :--- | :--- |
| `/bal [người chơi]` | Xem số dư tài khoản của bản thân hoặc người khác. | Mọi người chơi |
| `/pay <người chơi> <số tiền>` | Chuyển tiền cho người chơi khác (Tối thiểu 0.001 Xu). | Mọi người chơi |
| `/shop` | Mở Cửa hàng của Máy chủ. | Mọi người chơi |
| `/ah` | Mở Chợ đen - Sàn giao dịch tự do. | Mọi người chơi |
| `/eco <give/take/set> <mục tiêu> <số tiền>` | Can thiệp trực tiếp vào tài khoản người chơi. | **Admin (OP)** |

*(Lưu ý: Tất cả các lệnh trên đều có thể thay đổi tên gọi tiếng Việt trong file `config.json`).*

---

## 🧰 Cấu Hình (Configuration)

Mọi tệp cấu hình được tạo tự động tại thư mục `config/kyoeconomy/`:

### 1. `config.json`
Cho phép bạn đổi tên lệnh theo ý thích để phù hợp với ngữ cảnh Roleplay của máy chủ:
```json
{
  "command_bal": "tien",
  "command_pay": "chuyenkhoan",
  "command_shop": "cuahang",
  "command_ah": "choden"
}
### 2. `shop.json`
Quản lý các mặt hàng bán trong Admin Shop. Hỗ trợ hệ thống Giá biến động (Sàn chứng khoán) tự động nhảy giá ngẫu nhiên sau mỗi chu kỳ:
```json
{
  "items": [
    {
      "item_id": "minecraft:diamond",
      "name": "Kim Cương Mát Lạnh",
      "min_price": 45,
      "max_price": 55,
      "slot": 10
    }
  ]
}


