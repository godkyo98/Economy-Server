# 💰 Kyo Economy

![Minecraft Version](https://img.shields.io/badge/Minecraft-26.3-2ea44f?style=for-the-badge&logo=minecraft)
![Fabric Loader](https://img.shields.io/badge/Fabric%20Loader-0.19.5-dbd087?style=for-the-badge)
![Platform](https://img.shields.io/badge/Fabric-Server--Side-E3C95A?style=for-the-badge)
![API](https://img.shields.io/badge/Polymer-UI-blue?style=for-the-badge)

**Kyo Economy** là một hệ thống Kinh tế và Giao thương toàn diện, được thiết kế độc quyền cho **TEA Server**. Hoạt động hoàn toàn trên máy chủ (100% Server-side), người chơi không cần cài đặt thêm bất kỳ mod nào ở Client mà vẫn được trải nghiệm hệ thống Cửa hàng đa tầng phân trang, Chợ đen và hệ thống Thẩm định vật phẩm với trải nghiệm UI/UX mượt mà, trực quan.

---

## ✨ Tính Năng Nổi Bật

* 🛡️ **Kiến Trúc Dữ Liệu Tối Tân & An Toàn Vật Phẩm**: 
  * Loại bỏ hoàn toàn hệ thống NBT cũ kỹ dễ gây lỗi. Mod sử dụng công nghệ `DataFixerUpper (DFU)` kết hợp `RecordCodecBuilder` của Mojang để đọc/ghi dữ liệu siêu tốc, chống thất thoát tài sản tuyệt đối.
  * Toàn bộ cơ chế trả đồ/nhận đồ được chuyển sang spawn `ItemEntity` trực tiếp thay vì phụ thuộc vào Player drop API, loại bỏ triệt để nguy cơ mất đồ khi túi đồ đầy hoặc gặp sự cố mạng.
* 🌍 **Hệ Thống Lệnh Đa Ngôn Ngữ (Multi-Alias)**: Phá bỏ mọi rào cản ngôn ngữ! Cho phép gán vô số tên gọi cho cùng một lệnh (ví dụ: `/bal`, `/tien`, `/sodu`; `/shop`, `/cuahang`). Cực kỳ thân thiện cho các máy chủ đa quốc gia.
* 🛍️ **Giao Diện Đa Tầng & Phân Trang (Pagination)**: Cửa hàng được phân tách làm 6 Danh mục chuyên biệt với tên thân thiện (friendly names) và tích hợp sẵn hệ thống chuyển trang (Pagination), giúp duyệt hàng trăm món đồ dễ dàng, không bị tràn khung.
* ⚙️ **Cỗ Máy Thu Mua Đa Năng**: 
  * Hỗ trợ bán vật phẩm ngay trên con trỏ chuột (Cursor Sale).
  * Hỗ trợ thao tác thanh lý nhanh từ túi đồ và bán toàn bộ mặt hàng (Sell All).
  * Tự do sắp xếp túi đồ mà không lo bị bán nhầm.
* 🔍 **Thẩm Định Phù Phép Chuẩn Xác**: Cơ chế tính toán giá trị cộng thêm từ phù phép (Enchantment Bonus) được nâng cấp chặt chẽ và chuẩn hóa thành nhiều bậc giá trị, đi kèm hệ thống âm thanh và tin nhắn phản hồi sinh động khi kiểm tra giá.
* 📊 **Tối Ưu Hiển Thị Scoreboard**: Tự động rút gọn tiền tệ khổng lồ thành định dạng thông minh (`15.5k`, `1.2M`, `4.5B`) qua PlaceholderAPI, giữ cho Scoreboard & TabList luôn sạch sẽ và chống vỡ khung.

---

## 📦 Yêu Cầu Cài Đặt (Dependencies)

Để Kyo Economy hoạt động hoàn hảo, máy chủ của bạn cần cài đặt các mod lõi sau:
* **Fabric Loader** (`>= 0.19.5`)
* **Fabric API** (Phiên bản tương thích Minecraft 26.3)
* **Polymer** (Core & Resource Pack phiên bản mới nhất)
* **SGUI** (Server-side GUIs)
* **Placeholder API** (eu.pb4.placeholder-api v3+)

---

## 💻 Danh Sách Lệnh & Quyền Hạn (Commands)

| Lệnh mặc định | Chức năng | Quyền hạn (Permission) |
| :--- | :--- | :--- |
| `/bal [người chơi]` | Xem số dư tài khoản của bản thân hoặc người khác. | Mọi người chơi |
| `/pay <người chơi> <số tiền>` | Chuyển tiền cho người chơi khác (Tối thiểu 0.001 Xu). | Mọi người chơi |
| `/shop` | Mở Sảnh Cửa hàng chính của Máy chủ. | Mọi người chơi |
| `/ah` | Mở Chợ đen - Sàn giao dịch tự do giữa người chơi. | Mọi người chơi |
| `/eco <give/take/set> <mục tiêu> <số tiền>` | Can thiệp trực tiếp vào tài khoản người chơi. | **Admin (OP)** |

*(💡 Lưu ý: Danh sách trên là tên mặc định. Bạn có thể thiết lập nhiều tên lệnh khác nhau thông qua tệp `commands.json`.)*

---

## 🧰 Cấu Hình (Configuration)

Mọi tệp cấu hình được tạo tự động tại thư mục `config/kyoeconomy/`:

### 1. `commands.json` (Hệ thống Lệnh Đa Ngôn Ngữ)
Sử dụng mảng chuỗi (String Array) để cho phép người chơi gọi lệnh bằng nhiều cách khác nhau:
```json
{
  "cmd_bal": ["bal", "money", "sodu", "tien"],
  "cmd_pay": ["pay", "transfer", "chuyentien", "ck"],
  "cmd_shop": ["shop", "store", "cuahang", "chinhhang"],
  "cmd_ah": ["ah", "market", "choden", "chotroi"],
  "cmd_eco": ["eco", "economy", "quantritien"]
}
```

2. shop.json (Quản lý Mặt Hàng & Phân Trang Danh Mục)
Hỗ trợ hệ thống Giá biến động ngẫu nhiên theo chu kỳ và tự động phân loại mặt hàng vào các Tab danh mục:

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

## 💡 Danh sách 6 Tag Danh Mục hợp lệ:

blocks: Khối xây dựng

food: Nông sản & Thực phẩm

materials: Nguyên liệu & Khoáng sản

equipment: Vũ khí & Trang bị

mob_drops: Chiến lợi phẩm quái vật

special: Vật phẩm đặc biệt & Đồ hiếm

## 🎮 Hướng Dẫn Tương Tác Cửa Hàng (KyoShopGui)
Hệ thống cung cấp cơ chế tương tác trực quan ngay tại túi đồ người chơi (nửa dưới màn hình):

Chuột Trái Tự Do: Thoải mái cầm, chia đôi hoặc sắp xếp lại vật phẩm trong túi đồ mà không lo bị hệ thống kích hoạt bán nhầm.

Bán Trên Con Trỏ Chuột (Cursor Sale): Cầm vật phẩm trên con trỏ chuột và nhấp vào khu vực cửa hàng để bán ngay với giá niêm yết (tính kèm thưởng cấp độ phù phép).

Kiểm Tra Giá (Price Check): Nhấp Chuột Phải vào vật phẩm trong túi để in hóa đơn thẩm định chi tiết ra khung chat kèm hiệu ứng âm thanh.

Bán Nhanh An Toàn: Nhấp Shift + Chuột Phải để bán đứt ngay stack vật phẩm đó cho máy chủ.

Chuyển Trang (Pagination): Dễ dàng chuyển tiếp giữa các trang trong danh mục bằng các nút điều hướng trực quan ở hàng dưới cùng của GUI.

## 🔗 Hỗ Trợ PlaceholderAPI
Nhúng số dư người chơi vào Kyo Scoreboard, TabList hoặc Chat:

%kyoeconomy:balance%: Trả về số dư định dạng rút gọn thông minh (15.5k Xu, 2M Xu, 1B Xu). Tự động giữ nguyên định dạng lẻ (Hào) nếu tài sản dưới 1,000 Xu.

Phát triển bởi Kyo — Dành riêng cho Kỷ nguyên TEA Server.
