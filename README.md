# GoLike Helper — bản Android (bán tự động) v0.1

**Chưa được biên dịch hay thử trên máy thật** (nơi tạo mã không có Android SDK). Lần build đầu có thể báo lỗi nhỏ cần sửa.

## Nó làm gì
Dịch vụ Trợ năng đọc màn hình app GoLike và:
1. tự bấm nút *Nhận nhiệm vụ*;
2. hiện thanh nổi chờ bạn tự làm nhiệm vụ → bấm **✔ Đã làm xong** hoặc **Bỏ qua**;
3. chỉ khi bạn xác nhận mới quay lại GoLike và bấm nút *Hoàn thành*.

Có giới hạn/ngày, nghỉ ngẫu nhiên (≥5 giây), hết thời gian chờ thì bỏ qua và **không** báo hoàn thành.
Không tự like/follow/comment, không xử lý CAPTCHA.

## Tạo file APK (không cần cài Android Studio)
1. Tạo kho GitHub riêng tư, tải toàn bộ thư mục này lên (nhánh `main`).
2. Vào tab **Actions → Build APK → Run workflow**.
3. Khi xong, tải artifact `GoLikeHelper-debug-apk` → giải nén ra `app-debug.apk`.

Hoặc dùng Android Studio: *Open* thư mục này → *Build > Build APK(s)*.

## Cài và dùng
1. Chép APK sang điện thoại, cho phép cài từ nguồn ngoài, cài đặt.
2. Mở **GoLike Helper** → bấm *Tự tìm gói GoLike đã cài*.
3. Gõ đúng chữ hiển thị trên nút **Nhận** và nút **Hoàn thành** trong app GoLike (và nút Bỏ qua nếu có) → *Lưu*.
4. *Mở cài đặt Trợ năng* → bật **GoLike Helper**.
   - Android 13+ có thể báo «Cài đặt bị hạn chế»: vào *Cài đặt → Ứng dụng → GoLike Helper → ⋮ → Cho phép cài đặt bị hạn chế*, rồi bật lại.
5. Quay lại GoLike Helper → *Bật nhận nhiệm vụ* → mở app GoLike ở màn hình nhiệm vụ.

## Giới hạn đã biết
- Nếu app GoLike vẽ giao diện kiểu không có chữ cho Trợ năng đọc (một số app Flutter/game/WebView), dịch vụ sẽ không thấy nút.
- Nếu Android chặn tự mở lại GoLike, hãy tự bấm quay lại GoLike sau khi bấm «Đã làm xong».
- Chỉ dùng theo điều khoản của GoLike và các mạng xã hội liên quan.
