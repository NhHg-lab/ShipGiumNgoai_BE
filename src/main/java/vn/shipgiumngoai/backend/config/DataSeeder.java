package vn.shipgiumngoai.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.shipgiumngoai.backend.model.entity.*;
import vn.shipgiumngoai.backend.model.enums.Role;
import vn.shipgiumngoai.backend.model.enums.VerificationStatus;
import vn.shipgiumngoai.backend.repository.*;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DriverBlacklistRepository driverBlacklistRepository;
    private final VendorRepository vendorRepository;
    private final SystemShiftConfigRepository shiftConfigRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            DriverProfileRepository driverProfileRepository,
            DriverBlacklistRepository driverBlacklistRepository,
            VendorRepository vendorRepository,
            SystemShiftConfigRepository shiftConfigRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.driverProfileRepository = driverProfileRepository;
        this.driverBlacklistRepository = driverBlacklistRepository;
        this.vendorRepository = vendorRepository;
        this.shiftConfigRepository = shiftConfigRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (shiftConfigRepository.count() == 0) {
            SystemShiftConfig shift = new SystemShiftConfig();
            shift.setId(1L);
            shift.setShiftOpen(true);
            shift.setDefaultHours("18:00 - 23:00");
            shift.setTodayCustomHours("18:00 - 23:00");
            shift.setStatusBannerMessage("🟢 Đội ngũ sinh viên Ship Giùm Ngoại đang mở ca (18:00 - 23:00)! Đặt mua ủng hộ Ngoại về sớm nhé!");
            shift.setRiskFundBalance(150000L);
            shiftConfigRepository.save(shift);
        }

        if (userRepository.count() == 0) {
            String encodedPass = passwordEncoder.encode("123456");

            // 1. Tài khoản Khách hàng mẫu
            User customer = new User("0901111111", "Nguyễn Minh Khôi (Khách Hàng)", encodedPass, Role.CUSTOMER);
            customer.setCustomerWalletBalance(50000L);
            userRepository.save(customer);

            // 2. Tài khoản Tài xế mẫu (2 anh em sinh viên chạy ship)
            User driver = new User("0902222222", "Hoàng & Bạn (Shipper Sinh Viên)", encodedPass, Role.DRIVER);
            driver.setDriverWalletBalance(120000L);
            driver = userRepository.save(driver);

            DriverProfile driverProfile = new DriverProfile();
            driverProfile.setUser(driver);
            driverProfile.setCccdNumber("079203001234");
            driverProfile.setCccdFrontPath("private/hoang_cccd_front.jpg");
            driverProfile.setCccdBackPath("private/hoang_cccd_back.jpg");
            driverProfile.setVehiclePlate("59-T2 686.86");
            driverProfile.setCashOnHand(350000L);
            driverProfile.setOnline(true);
            driverProfile.setVerificationStatus(VerificationStatus.APPROVED);
            driverProfile.setInterviewNotes("Thành viên sáng lập - Đã xác thực 100%");
            driverProfile.setCurrentLat(10.7755);
            driverProfile.setCurrentLng(106.6820);
            driverProfileRepository.save(driverProfile);

            // 3. Tài khoản Admin
            User admin = new User("0909999999", "Admin Ship Giùm Ngoại", encodedPass, Role.ADMIN);
            userRepository.save(admin);
        }

        // Seed 1 CCCD trong Blacklist để test tính năng Auto-Kick + Ghi log báo Công an
        if (driverBlacklistRepository.count() == 0) {
            driverBlacklistRepository.save(new DriverBlacklist(
                    "079099888777",
                    "0908888888",
                    "Trần Văn Gian Lận",
                    "Đủ 3 gậy: Thái độ hỗn láo nói xấu Ngoại và gian lận hóa đơn"
            ));
        }

        if (vendorRepository.count() == 0) {
            // Ngoại 1: Ngoại Tư bán bánh xèo
            Vendor v1 = new Vendor();
            v1.setName("Ngoại Tư - Bánh Xèo & Bánh Khọt Đêm");
            v1.setStory("Ngoại Tư năm nay 78 tuổi, một mình nuôi cháu ngoại ăn học. Mỗi tối từ 18h Ngoại ngồi đổ bánh xèo bên góc đường Nguyễn Thượng Hiền, chỉ mong bán hết sớm trước 11h đêm để về nghỉ lưng.");
            v1.setAddressText("214 Nguyễn Thượng Hiền, Phường 4, Quận 3, TP.HCM");
            v1.setLatitude(10.7762);
            v1.setLongitude(106.6838);
            v1.setSellingHours("18:00 - 22:30");
            v1.setAvatarUrl("https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=600&auto=format&fit=crop&q=80");
            v1.setContributorName("@SaiGonBaoDung (TikTok)");
            v1.setAnonymousContributor(false);
            v1.setSourceVideoUrl("https://www.tiktok.com/@saigonbaodung");
            v1.setApprovalStatus("APPROVED");
            v1.addDish(new VendorDish("Bánh xèo miền Tây (1 cái lớn)", 25000L, null));
            v1.addDish(new VendorDish("Phần 6 bánh khọt nước cốt dừa", 30000L, null));
            v1.addDish(new VendorDish("Combo ủng hộ Ngoại (2 Bánh xèo + 1 Bánh khọt)", 75000L, null));
            vendorRepository.save(v1);

            // Ngoại 2: Cụ Bảy bán chè nóng (Ẩn danh người gieo duyên -> hiện "Một người tốt ẩn danh")
            Vendor v2 = new Vendor();
            v2.setName("Ngoại Bảy - Chè Đậu Trắng & Chuối Chưng Nóng");
            v2.setStory("Ngoại Bảy hơn 80 tuổi, mắt đã mờ và không có điện thoại. Nồi chè đậu trắng nước cốt dừa Ngoại nấu bằng bếp củi thơm lừng, mỗi bịch chỉ 15k-20k.");
            v2.setAddressText("88 Cao Thắng, Phường 4, Quận 3, TP.HCM (Cách Ngoại Tư 600m)");
            v2.setLatitude(10.7728);
            v2.setLongitude(106.6815);
            v2.setSellingHours("18:30 - 23:00");
            v2.setAvatarUrl("https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=600&auto=format&fit=crop&q=80");
            v2.setAnonymousContributor(true);
            v2.setApprovalStatus("APPROVED");
            v2.addDish(new VendorDish("Chè đậu trắng nước cốt dừa nóng", 20000L, null));
            v2.addDish(new VendorDish("Chè chuối chưng bột báng", 15000L, null));
            v2.addDish(new VendorDish("Combo 3 phần chè ấm lòng đêm khuya", 50000L, null));
            vendorRepository.save(v2);

            // Ngoại 3: Ông Sáu bán bánh bao & bắp xào (Cũng gần Ngoại Tư ~900m để test Option 3 đổi sang món đắt hơn/rẻ hơn)
            Vendor v3 = new Vendor();
            v3.setName("Ông Sáu - Bánh Bao Nóng & Bắp Xào Tép Đêm");
            v3.setStory("Ông Sáu đẩy xe bánh bao và bắp xào mưu sinh nuôi bà cụ bị tai biến ở nhà trọ. Ông hiền lành, ai mua cũng cười tươi cảm ơn.");
            v3.setAddressText("145 Điện Biên Phủ, Quận 3, TP.HCM (Cách Ngoại Tư 900m)");
            v3.setLatitude(10.7810);
            v3.setLongitude(106.6875);
            v3.setSellingHours("18:00 - 23:30");
            v3.setAvatarUrl("https://images.unsplash.com/photo-1496116218417-1a781b1c416c?w=600&auto=format&fit=crop&q=80");
            v3.setContributorName("Nguyễn Lan Anh");
            v3.setAnonymousContributor(false);
            v3.setApprovalStatus("APPROVED");
            v3.addDish(new VendorDish("Bánh bao nhân thịt trứng cút", 20000L, null));
            v3.addDish(new VendorDish("Hộp bắp xào bơ tép đầy ắp", 25000L, null));
            v3.addDish(new VendorDish("Combo đêm khuya (2 Bánh bao + 1 Bắp xào)", 60000L, null));
            vendorRepository.save(v3);

            // Ngoại 4: Đang chờ Admin duyệt (PENDING) do cộng đồng gửi lên
            Vendor v4 = new Vendor();
            v4.setName("Bà Năm - Hủ Tiếu Gõ & Xôi Mặn Khuya");
            v4.setStory("Bà Năm bán xôi mặn lá chuối ở góc ngã tư Sư Vạn Hạnh từ 7h tối đến khuya, xôi rất dẻo và ngon.");
            v4.setAddressText("310 Sư Vạn Hạnh, Quận 10, TP.HCM");
            v4.setLatitude(10.7740);
            v4.setLongitude(106.6675);
            v4.setSellingHours("19:00 - 23:00");
            v4.setAnonymousContributor(true);
            v4.setApprovalStatus("PENDING");
            v4.addDish(new VendorDish("Hộp xôi mặn thập cẩm lá chuối", 25000L, null));
            vendorRepository.save(v4);
        }
    }
}
