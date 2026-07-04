# 💰 Kyo Economy

![Minecraft Version](https://img.shields.io/badge/Minecraft-26.2-2ea44f?style=for-the-badge&logo=minecraft)
![Platform](https://img.shields.io/badge/Fabric-Server--Side-E3C95A?style=for-the-badge)
![API](https://img.shields.io/badge/Polymer-UI-blue?style=for-the-badge)

**Kyo Economy** là một hệ thống Kinh tế và Giao thương toàn diện, được thiết kế độc quyền cho **TEA Server**. Chạy hoàn toàn trên máy chủ (100% Server-side), người chơi không cần cài đặt thêm bất kỳ mod nào ở Client mà vẫn được trải nghiệm hệ thống Cửa hàng đa tầng, Chợ đen và hệ thống Thẩm định vật phẩm với trải nghiệm UI/UX cực kỳ chuyên nghiệp.

---

## ✨ Tính Năng Nổi Bật

* 🛡️ **Kiến Trúc Dữ Liệu Tối Tân**: Loại bỏ hoàn toàn hệ thống NBT cũ kỹ dễ gây lỗi. Kyo Economy sử dụng công nghệ `DataFixerUpper (DFU)` kết hợp `RecordCodecBuilder` mới nhất của Mojang để đọc/ghi dữ liệu siêu tốc, chống thất thoát tài sản tuyệt đối.
* 🌍 **Hệ Thống Lệnh Đa Ngôn Ngữ (Multi-Alias)**: Phá bỏ mọi rào cản ngôn ngữ! Cho phép gán vô số tên gọi cho cùng một lệnh (Ví dụ: `/bal` và `/sodu`, `/shop` và `/cuahang`). Cực kỳ thân thiện cho các máy chủ có cả người chơi trong nước và quốc tế.
* 🛍️ **Giao Diện Đa Tầng (Nested Menu)**: Trải nghiệm mua bán chuẩn RPG. Cửa hàng được phân tách làm 6 Danh mục chuyên biệt (Khối xây dựng, Nông sản, Vũ khí, Chiến lợi phẩm...) giúp người chơi dễ dàng tìm kiếm mà không cần lật trang mỏi mệt.
* ⚙️ **Cỗ Máy Thu Mua Vạn Năng**: Thiết kế tối ưu hóa UX đỉnh cao! Cho phép **Kéo & Thả (Drag & Drop)** vật phẩm trực tiếp vào cửa hàng để thanh lý, hoặc sử dụng tổ hợp phím túi đồ an toàn (như Shift + Chuột Phải) để bán đứt mà không sợ lỡ tay ấn nhầm khi đang sắp xếp rương.
* 🔍 **Thẩm Định Phù Phép**: Cơ chế độc quyền cho phép định giá các trang bị có phù phép. Giá trị được phân chia từ Bậc cơ bản đến Thần Cấp, mang lại giá trị cày cuốc cực cao cho các hoạt động PvE và Câu cá.
* 📊 **Tối Ưu Hiển Thị Scoreboard**: Tự động rút gọn tiền tệ khổng lồ thành định dạng thông minh (`15.5k`, `1.2M`, `4.5B`) qua PlaceholderAPI, giữ cho Scoreboard & TabList luôn sạch sẽ và chống vỡ khung.

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
| `/shop` | Mở Sảnh Cửa hàng của Máy chủ. | Mọi người chơi |
| `/ah` | Mở Chợ đen - Sàn giao dịch tự do. | Mọi người chơi |
| `/eco <give/take/set> <mục tiêu> <số tiền>` | Can thiệp trực tiếp vào tài khoản người chơi. | **Admin (OP)** |

*(💡 Lưu ý: Danh sách trên chỉ là tên gọi gốc. Bạn có thể sử dụng cấu trúc Mảng (Array) trong file cấu hình để đăng ký hàng loạt các tên lệnh thay thế (alias) theo ý thích!)*

---

## 🧰 Cấu Hình (Configuration)

Mọi tệp cấu hình được tạo tự động tại thư mục `config/kyoeconomy/`:

### 1. `commands.json` (Hệ thống Lệnh Đa Ngôn Ngữ)
Sử dụng mảng chuỗi (String Array) để cho phép người chơi gọi lệnh bằng nhiều cách khác nhau. Rất tiện lợi cho Server đa quốc gia:
```json
{
  "cmd_bal": ["bal", "money", "sodu", "tien"],
  "cmd_pay": ["pay", "transfer", "chuyentien", "ck"],
  "cmd_shop": ["shop", "store", "cuahang", "chinhhang"],
  "cmd_ah": ["ah", "market", "choden", "chotroi"],
  "cmd_eco": ["eco", "economy", "quantritien"]
}
```
### 2. `shop.json` (Quản lý Cửa Hàng & Sàn Chứng Khoán)
   Hỗ trợ hệ thống Giá biến động (tự động nhảy giá ngẫu nhiên sau mỗi chu kỳ) và tự động phân luồng vào 6 Tab danh mục bằng thuộc tính "category":

```JSON
{
  "items": [
    {
      "item_id": "minecraft:cobblestone",
      "name": "Đá Cuội Xây Dựng",
      "min_price": 5.0,
      "max_price": 10.0,
      "category": "blocks"
    },
    {
      "item_id": "minecraft:diamond_sword",
      "name": "Kiếm Kim Cương",
      "price": 500.0,
      "category": "equipment"
    }
  ]
}
```
* 💡 Danh sách 6 Tag Danh Mục hợp lệ: blocks (Khối xây), food (Thực phẩm), materials (Nguyên liệu), equipment (Trang bị), mob_drops (Đồ quái vật rớt), special (Đồ hiếm).

##  🎮 Hướng Dẫn Tương Tác Cửa Hàng Chuyên Nghiệp
Để giúp việc thanh lý đồ đạc rác trong túi trở nên dễ dàng nhất, hệ thống Cửa hàng Kyo cung cấp 3 cơ chế tương tác ở Khung Hành Trang của người chơi (nửa dưới màn hình):

* **Chuột Trái Tự Do**: Bạn có thể thoải mái click Chuột Trái để cầm, chia đôi, hoặc sắp xếp lại vật phẩm trong túi đồ của mình mà không sợ hệ thống vô tình "bán nhầm" đồ của bạn.

* **Kéo & Thả (Drag & Drop)**: Hãy cầm một vật phẩm hoặc một stack từ túi đồ, nhấp thẳng vào bất kỳ ô trống nào trên Cửa Hàng. Hệ thống sẽ tự động thu mua nó với giá niêm yết (10% giá gốc + Phù phép)!

* **Thao Tác Nhanh** (Fast Actions): Đưa chuột vào đồ trong túi và sử dụng:

* * **🖱️ Chuột Phải**: Thẩm định giá. In ra hóa đơn chi tiết trên Chat xem món đồ này bán được bao nhiêu Xu (đặc biệt hữu dụng để tính giá đồ Phù phép).

* * **⌨️ Shift + Chuột Phải**: Bán Đứt. Bán thẳng toàn bộ (1 stack) mặt hàng đó vào máy chủ cực kỳ an toàn.

## 🔗 Hỗ Trợ PlaceholderAPI
Sử dụng biến số dưới đây để nhúng số dư của người chơi vào Kyo Scoreboard, TabList hoặc khung Chat:

* **%kyoeconomy:balance%**: Trả về số tiền cực kỳ gọn gàng (15.5k Xu, 2M Xu, 1B Xu). Giữ nguyên định dạng lẻ (Hào) nếu tài sản dưới 1,000 Xu.

# Phát triển bởi Kyo - Dành riêng cho Kỷ nguyên TEA Server.