package com.toeicflashcard.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class TopicVisual(
    val icon: ImageVector,
    val subtitleVi: String
)

private val DefaultTopicVisual = TopicVisual(Icons.Rounded.BookmarkAdded, "Chủ đề tự tạo")

private val TopicVisualMap: Map<String, TopicVisual> = mapOf(
    "Business & Office" to TopicVisual(Icons.Rounded.BusinessCenter, "Kinh doanh & Văn phòng"),
    "Finance & Banking" to TopicVisual(Icons.Rounded.AccountBalance, "Tài chính & Ngân hàng"),
    "Travel & Logistics" to TopicVisual(Icons.Rounded.FlightTakeoff, "Du lịch & Vận tải"),
    "Business Contract & Deal" to TopicVisual(Icons.Rounded.Handshake, "Hợp đồng & Giao dịch"),
    "Marketing" to TopicVisual(Icons.Rounded.Campaign, "Tiếp thị & Quảng bá"),
    "Business Planning & Report" to TopicVisual(Icons.Rounded.Assessment, "Kế hoạch & Báo cáo"),
    "Conferences" to TopicVisual(Icons.Rounded.Groups, "Hội nghị & Hội thảo"),
    "Office Technology" to TopicVisual(Icons.Rounded.Computer, "Công nghệ văn phòng"),
    "Office Procedures" to TopicVisual(Icons.AutoMirrored.Rounded.ListAlt, "Quy trình văn phòng"),
    "Electronics & Appliance" to TopicVisual(Icons.Rounded.Devices, "Điện tử & Gia dụng"),
    "Correspondence" to TopicVisual(Icons.Rounded.Mail, "Thư tín & Trao đổi"),
    "Job Advertising & Recruiting" to TopicVisual(Icons.Rounded.WorkOutline, "Tuyển dụng & Tìm việc"),
    "Applying & Interviewing" to TopicVisual(Icons.Rounded.Badge, "Ứng tuyển & Phỏng vấn"),
    "Hiring & Training" to TopicVisual(Icons.Rounded.School, "Tuyển mới & Đào tạo"),
    "Salaries & Benefits" to TopicVisual(Icons.Rounded.Paid, "Lương thưởng & Phúc lợi"),
    "Promotions, Pensions & Awards" to TopicVisual(Icons.Rounded.EmojiEvents, "Thăng tiến & Khen thưởng"),
    "Shopping & Supplies" to TopicVisual(Icons.Rounded.ShoppingBag, "Mua sắm & Cung ứng"),
    "Warranty & Maintenance" to TopicVisual(Icons.Rounded.Build, "Bảo hành & Bảo trì"),
    "Shipping & Transportation" to TopicVisual(Icons.Rounded.LocalShipping, "Vận chuyển & Giao thông"),
    "Invoices, Accounting & Taxes" to TopicVisual(Icons.AutoMirrored.Rounded.ReceiptLong, "Hóa đơn, Kế toán & Thuế"),
    "Finance & Investment" to TopicVisual(Icons.AutoMirrored.Rounded.TrendingUp, "Tài chính & Đầu tư"),
    "Property & Facility" to TopicVisual(Icons.Rounded.Apartment, "Bất động sản & Cơ sở"),
    "Board Meetings & Committees" to TopicVisual(Icons.AutoMirrored.Rounded.Assignment, "Họp hội đồng & Ủy ban"),
    "Quality Control" to TopicVisual(Icons.AutoMirrored.Rounded.FactCheck, "Kiểm soát chất lượng"),
    "Product Development" to TopicVisual(Icons.Rounded.Description, "Phát triển sản phẩm"),
    "Renting & Leasing" to TopicVisual(Icons.Rounded.Key, "Thuê & Cho thuê"),
    "Customer Service" to TopicVisual(Icons.Rounded.SupportAgent, "Chăm sóc khách hàng"),
    "Factory" to TopicVisual(Icons.Rounded.PrecisionManufacturing, "Nhà máy & Sản xuất"),
    "Art, Science & Museum" to TopicVisual(Icons.Rounded.Museum, "Nghệ thuật, Khoa học & Bảo tàng"),
    "Environment & Weather" to TopicVisual(Icons.Rounded.Public, "Môi trường & Thời tiết"),
    "Government & Public Facility" to TopicVisual(Icons.Rounded.Domain, "Chính phủ & Công trình công"),
    "Dining & Restaurant" to TopicVisual(Icons.Rounded.Restaurant, "Ẩm thực & Nhà hàng"),
    "Indoor & Appliance" to TopicVisual(Icons.Rounded.Weekend, "Đồ gia dụng trong nhà"),
    "Materials & Furniture" to TopicVisual(Icons.Rounded.Chair, "Vật liệu & Nội thất"),
    "Fashion & Clothing" to TopicVisual(Icons.Rounded.Checkroom, "Thời trang & Trang phục"),
    "Outdoor & Landscaping" to TopicVisual(Icons.Rounded.Park, "Ngoài trời & Cảnh quan"),

    // 600 Essential Words For The TOEIC Test (L01..L50)
    "L01 • Contracts" to TopicVisual(Icons.Rounded.Handshake, "Bài 01 • Hợp đồng & Giao dịch"),
    "L02 • Marketing" to TopicVisual(Icons.Rounded.Campaign, "Bài 02 • Tiếp thị & Quảng bá"),
    "L03 • Warranties" to TopicVisual(Icons.Rounded.Build, "Bài 03 • Bảo hành & Cam kết"),
    "L04 • Business Planning" to TopicVisual(Icons.Rounded.Assessment, "Bài 04 • Hoạch định kinh doanh"),
    "L05 • Conferences" to TopicVisual(Icons.Rounded.Groups, "Bài 05 • Hội nghị & Hội thảo"),
    "L06 • Computers" to TopicVisual(Icons.Rounded.Computer, "Bài 06 • Máy tính & Phần mềm"),
    "L07 • Office Technology" to TopicVisual(Icons.Rounded.Devices, "Bài 07 • Công nghệ văn phòng"),
    "L08 • Office Procedures" to TopicVisual(Icons.AutoMirrored.Rounded.ListAlt, "Bài 08 • Quy trình văn phòng"),
    "L09 • Electronics" to TopicVisual(Icons.Rounded.Devices, "Bài 09 • Thiết bị điện tử"),
    "L10 • Correspondence" to TopicVisual(Icons.Rounded.Mail, "Bài 10 • Thư tín & Trao đổi"),
    "L11 • Job Advertising & Recruiting" to TopicVisual(Icons.Rounded.WorkOutline, "Bài 11 • Quảng cáo tuyển dụng"),
    "L12 • Applying & Interviewing" to TopicVisual(Icons.Rounded.Badge, "Bài 12 • Ứng tuyển & Phỏng vấn"),
    "L13 • Hiring & Training" to TopicVisual(Icons.Rounded.School, "Bài 13 • Tuyển dụng & Đào tạo"),
    "L14 • Salaries & Benefits" to TopicVisual(Icons.Rounded.Paid, "Bài 14 • Lương thưởng & Phúc lợi"),
    "L15 • Promotions, Pensions & Awards" to TopicVisual(Icons.Rounded.EmojiEvents, "Bài 15 • Thăng tiến & Khen thưởng"),
    "L16 • Shopping" to TopicVisual(Icons.Rounded.ShoppingBag, "Bài 16 • Mua sắm & Bán lẻ"),
    "L17 • Ordering Supplies" to TopicVisual(Icons.Rounded.ShoppingBag, "Bài 17 • Đặt hàng vật tư"),
    "L18 • Shipping" to TopicVisual(Icons.Rounded.LocalShipping, "Bài 18 • Giao nhận & Vận chuyển"),
    "L19 • Invoices" to TopicVisual(Icons.AutoMirrored.Rounded.ReceiptLong, "Bài 19 • Hóa đơn & Thanh toán"),
    "L20 • Inventory" to TopicVisual(Icons.Rounded.Inventory2, "Bài 20 • Kiểm kê & Hàng tồn kho"),
    "L21 • Banking" to TopicVisual(Icons.Rounded.AccountBalance, "Bài 21 • Ngân hàng & Giao dịch"),
    "L22 • Accounting" to TopicVisual(Icons.AutoMirrored.Rounded.ReceiptLong, "Bài 22 • Kế toán & Sổ sách"),
    "L23 • Investments" to TopicVisual(Icons.AutoMirrored.Rounded.TrendingUp, "Bài 23 • Đầu tư & Cổ phiếu"),
    "L24 • Taxes" to TopicVisual(Icons.Rounded.AccountBalance, "Bài 24 • Thuế & Khai báo"),
    "L25 • Financial Statements" to TopicVisual(Icons.Rounded.Assessment, "Bài 25 • Báo cáo tài chính"),
    "L26 • Property & Department" to TopicVisual(Icons.Rounded.Apartment, "Bài 26 • Cơ sở vật chất & Phòng ban"),
    "L27 • Board Meetings & Committees" to TopicVisual(Icons.AutoMirrored.Rounded.Assignment, "Bài 27 • Họp hội đồng & Ủy ban"),
    "L28 • Quality Control" to TopicVisual(Icons.AutoMirrored.Rounded.FactCheck, "Bài 28 • Kiểm soát chất lượng"),
    "L29 • Product Development" to TopicVisual(Icons.Rounded.Description, "Bài 29 • Phát triển sản phẩm"),
    "L30 • Renting & Leasing" to TopicVisual(Icons.Rounded.Key, "Bài 30 • Thuê & Cho thuê"),
    "L31 • Selecting a Restaurant" to TopicVisual(Icons.Rounded.Restaurant, "Bài 31 • Lựa chọn nhà hàng"),
    "L32 • Eating Out" to TopicVisual(Icons.Rounded.Restaurant, "Bài 32 • Dùng bữa bên ngoài"),
    "L33 • Ordering Lunch" to TopicVisual(Icons.Rounded.Restaurant, "Bài 33 • Đặt bữa trưa"),
    "L34 • Cooking as a Career" to TopicVisual(Icons.Rounded.Restaurant, "Bài 34 • Nghề bếp & Ẩm thực"),
    "L35 • Events" to TopicVisual(Icons.Rounded.Groups, "Bài 35 • Sự kiện & Tổ chức"),
    "L36 • General Travel" to TopicVisual(Icons.Rounded.FlightTakeoff, "Bài 36 • Du lịch tổng quát"),
    "L37 • Airlines" to TopicVisual(Icons.Rounded.FlightTakeoff, "Bài 37 • Hàng không & Chuyến bay"),
    "L38 • Trains" to TopicVisual(Icons.Rounded.Train, "Bài 38 • Tàu hỏa & Đường sắt"),
    "L39 • Hotels" to TopicVisual(Icons.Rounded.Hotel, "Bài 39 • Khách sạn & Lưu trú"),
    "L40 • Car Rentals" to TopicVisual(Icons.Rounded.DirectionsCar, "Bài 40 • Thuê xe & Di chuyển"),
    "L41 • Movies" to TopicVisual(Icons.Rounded.Movie, "Bài 41 • Phim ảnh & Rạp chiếu"),
    "L42 • Theater" to TopicVisual(Icons.Rounded.TheaterComedy, "Bài 42 • Nhà hát & Sân khấu"),
    "L43 • Music" to TopicVisual(Icons.Rounded.MusicNote, "Bài 43 • Âm nhạc & Hòa nhạc"),
    "L44 • Museums" to TopicVisual(Icons.Rounded.Museum, "Bài 44 • Bảo tàng & Triển lãm"),
    "L45 • Media" to TopicVisual(Icons.Rounded.Newspaper, "Bài 45 • Truyền thông & Báo chí"),
    "L46 • Doctor's Office" to TopicVisual(Icons.Rounded.MedicalServices, "Bài 46 • Phòng khám bác sĩ"),
    "L47 • Dentist's Office" to TopicVisual(Icons.Rounded.MedicalServices, "Bài 47 • Phòng khám nha khoa"),
    "L48 • Health Insurance" to TopicVisual(Icons.Rounded.HealthAndSafety, "Bài 48 • Bảo hiểm y tế"),
    "L49 • Hospitals" to TopicVisual(Icons.Rounded.LocalHospital, "Bài 49 • Bệnh viện & Điều trị"),
    "L50 • Pharmacy" to TopicVisual(Icons.Rounded.LocalHospital, "Bài 50 • Nhà thuốc & Dược phẩm"),

    // 800 Từ TOEIC Level 450 (Part 1..Part 5-6)
    "Part 1 • Hành động & Việc làm" to TopicVisual(Icons.Rounded.Headphones, "Nghe Part 1 • Động tác & Thao tác"),
    "Part 1 • Đồ vật & Địa điểm" to TopicVisual(Icons.Rounded.Headphones, "Nghe Part 1 • Bối cảnh & Đồ vật"),
    "Part 1 • Trạng thái & Vị trí" to TopicVisual(Icons.Rounded.Headphones, "Nghe Part 1 • Trạng thái & Vị trí"),
    "Part 2 • Chủ điểm Người" to TopicVisual(Icons.Rounded.RecordVoiceOver, "Nghe Part 2 • Câu hỏi Who / Whom"),
    "Part 2 • Chủ điểm Thời gian" to TopicVisual(Icons.Rounded.RecordVoiceOver, "Nghe Part 2 • Câu hỏi When"),
    "Part 2 • Chủ điểm Nơi chốn" to TopicVisual(Icons.Rounded.RecordVoiceOver, "Nghe Part 2 • Câu hỏi Where"),
    "Part 2 • Chủ điểm Lý do" to TopicVisual(Icons.Rounded.RecordVoiceOver, "Nghe Part 2 • Câu hỏi Why"),
    "Part 2 • Chủ điểm Cách làm" to TopicVisual(Icons.Rounded.RecordVoiceOver, "Nghe Part 2 • Câu hỏi How"),
    "Part 2 • Chủ điểm Lựa chọn" to TopicVisual(Icons.Rounded.RecordVoiceOver, "Nghe Part 2 • Câu hỏi Lựa chọn"),
    "Part 2 • Trả lời cảm thán" to TopicVisual(Icons.Rounded.RecordVoiceOver, "Nghe Part 2 • Trần thuật & Cảm thán"),
    "Part 2 • Trả lời không biết" to TopicVisual(Icons.Rounded.RecordVoiceOver, "Nghe Part 2 • Phản hồi gián tiếp"),
    "Part 3 • Nơi làm việc 1" to TopicVisual(Icons.Rounded.Groups, "Hội thoại Part 3 • Công sở (Phần 1)"),
    "Part 3 • Nơi làm việc 2" to TopicVisual(Icons.Rounded.Groups, "Hội thoại Part 3 • Công sở (Phần 2)"),
    "Part 3 • Mua sắm & Di chuyển" to TopicVisual(Icons.Rounded.ShoppingBag, "Hội thoại Part 3 • Giao dịch & Đi lại"),
    "Part 3 • Nhà hàng, Y tế & Ngân hàng" to TopicVisual(Icons.Rounded.Restaurant, "Hội thoại Part 3 • Dịch vụ & Y tế"),
    "Part 5-6 • Trọng tâm Phần 1 (1–100)" to TopicVisual(Icons.AutoMirrored.Rounded.MenuBook, "Đọc Part 5-6 • Cụm trọng tâm 1–100"),
    "Part 5-6 • Trọng tâm Phần 2 (101–200)" to TopicVisual(Icons.AutoMirrored.Rounded.MenuBook, "Đọc Part 5-6 • Cụm trọng tâm 101–200"),
    "Part 5-6 • Trọng tâm Phần 3 (201–300)" to TopicVisual(Icons.AutoMirrored.Rounded.MenuBook, "Đọc Part 5-6 • Cụm trọng tâm 201–300")
)

fun topicVisualFor(topic: String): TopicVisual = TopicVisualMap[topic] ?: DefaultTopicVisual

fun topicSubtitleViFor(topic: String): String = topicVisualFor(topic).subtitleVi

val TOPIC_CATEGORIES: List<String> = listOf(
    "Kinh doanh & Văn phòng",
    "Tài chính & Kế toán",
    "Nhân sự & Tuyển dụng",
    "Tiếp thị & Khách hàng",
    "Sản xuất, Vận tải & Du lịch",
    "Đời sống & Công trình",
    "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Luyện Đọc Part 5–6 (TOEIC 450)"
)

private val TopicCategoryMap: Map<String, String> = mapOf(
    "Business & Office" to "Kinh doanh & Văn phòng",
    "Business Contract & Deal" to "Kinh doanh & Văn phòng",
    "Business Planning & Report" to "Kinh doanh & Văn phòng",
    "Conferences" to "Kinh doanh & Văn phòng",
    "Office Technology" to "Kinh doanh & Văn phòng",
    "Office Procedures" to "Kinh doanh & Văn phòng",
    "Correspondence" to "Kinh doanh & Văn phòng",
    "Board Meetings & Committees" to "Kinh doanh & Văn phòng",
    "Finance & Banking" to "Tài chính & Kế toán",
    "Invoices, Accounting & Taxes" to "Tài chính & Kế toán",
    "Finance & Investment" to "Tài chính & Kế toán",
    "Job Advertising & Recruiting" to "Nhân sự & Tuyển dụng",
    "Applying & Interviewing" to "Nhân sự & Tuyển dụng",
    "Hiring & Training" to "Nhân sự & Tuyển dụng",
    "Salaries & Benefits" to "Nhân sự & Tuyển dụng",
    "Promotions, Pensions & Awards" to "Nhân sự & Tuyển dụng",
    "Marketing" to "Tiếp thị & Khách hàng",
    "Shopping & Supplies" to "Tiếp thị & Khách hàng",
    "Warranty & Maintenance" to "Tiếp thị & Khách hàng",
    "Product Development" to "Tiếp thị & Khách hàng",
    "Customer Service" to "Tiếp thị & Khách hàng",
    "Travel & Logistics" to "Sản xuất, Vận tải & Du lịch",
    "Shipping & Transportation" to "Sản xuất, Vận tải & Du lịch",
    "Quality Control" to "Sản xuất, Vận tải & Du lịch",
    "Factory" to "Sản xuất, Vận tải & Du lịch",
    "Electronics & Appliance" to "Đời sống & Công trình",
    "Property & Facility" to "Đời sống & Công trình",
    "Renting & Leasing" to "Đời sống & Công trình",
    "Art, Science & Museum" to "Đời sống & Công trình",
    "Environment & Weather" to "Đời sống & Công trình",
    "Government & Public Facility" to "Đời sống & Công trình",
    "Dining & Restaurant" to "Đời sống & Công trình",
    "Indoor & Appliance" to "Đời sống & Công trình",
    "Materials & Furniture" to "Đời sống & Công trình",
    "Fashion & Clothing" to "Đời sống & Công trình",
    "Outdoor & Landscaping" to "Đời sống & Công trình",

    // 600 Essential Words categories
    "L01 • Contracts" to "Kinh doanh & Văn phòng",
    "L04 • Business Planning" to "Kinh doanh & Văn phòng",
    "L05 • Conferences" to "Kinh doanh & Văn phòng",
    "L06 • Computers" to "Kinh doanh & Văn phòng",
    "L07 • Office Technology" to "Kinh doanh & Văn phòng",
    "L08 • Office Procedures" to "Kinh doanh & Văn phòng",
    "L10 • Correspondence" to "Kinh doanh & Văn phòng",
    "L26 • Property & Department" to "Kinh doanh & Văn phòng",
    "L27 • Board Meetings & Committees" to "Kinh doanh & Văn phòng",
    "L19 • Invoices" to "Tài chính & Kế toán",
    "L21 • Banking" to "Tài chính & Kế toán",
    "L22 • Accounting" to "Tài chính & Kế toán",
    "L23 • Investments" to "Tài chính & Kế toán",
    "L24 • Taxes" to "Tài chính & Kế toán",
    "L25 • Financial Statements" to "Tài chính & Kế toán",
    "L11 • Job Advertising & Recruiting" to "Nhân sự & Tuyển dụng",
    "L12 • Applying & Interviewing" to "Nhân sự & Tuyển dụng",
    "L13 • Hiring & Training" to "Nhân sự & Tuyển dụng",
    "L14 • Salaries & Benefits" to "Nhân sự & Tuyển dụng",
    "L15 • Promotions, Pensions & Awards" to "Nhân sự & Tuyển dụng",
    "L02 • Marketing" to "Tiếp thị & Khách hàng",
    "L03 • Warranties" to "Tiếp thị & Khách hàng",
    "L16 • Shopping" to "Tiếp thị & Khách hàng",
    "L17 • Ordering Supplies" to "Tiếp thị & Khách hàng",
    "L29 • Product Development" to "Tiếp thị & Khách hàng",
    "L18 • Shipping" to "Sản xuất, Vận tải & Du lịch",
    "L20 • Inventory" to "Sản xuất, Vận tải & Du lịch",
    "L28 • Quality Control" to "Sản xuất, Vận tải & Du lịch",
    "L36 • General Travel" to "Sản xuất, Vận tải & Du lịch",
    "L37 • Airlines" to "Sản xuất, Vận tải & Du lịch",
    "L38 • Trains" to "Sản xuất, Vận tải & Du lịch",
    "L39 • Hotels" to "Sản xuất, Vận tải & Du lịch",
    "L40 • Car Rentals" to "Sản xuất, Vận tải & Du lịch",
    "L09 • Electronics" to "Đời sống & Công trình",
    "L30 • Renting & Leasing" to "Đời sống & Công trình",
    "L31 • Selecting a Restaurant" to "Đời sống & Công trình",
    "L32 • Eating Out" to "Đời sống & Công trình",
    "L33 • Ordering Lunch" to "Đời sống & Công trình",
    "L34 • Cooking as a Career" to "Đời sống & Công trình",
    "L35 • Events" to "Đời sống & Công trình",
    "L41 • Movies" to "Đời sống & Công trình",
    "L42 • Theater" to "Đời sống & Công trình",
    "L43 • Music" to "Đời sống & Công trình",
    "L44 • Museums" to "Đời sống & Công trình",
    "L45 • Media" to "Đời sống & Công trình",
    "L46 • Doctor's Office" to "Đời sống & Công trình",
    "L47 • Dentist's Office" to "Đời sống & Công trình",
    "L48 • Health Insurance" to "Đời sống & Công trình",
    "L49 • Hospitals" to "Đời sống & Công trình",
    "L50 • Pharmacy" to "Đời sống & Công trình",

    // 800 Từ TOEIC Level 450 categories
    "Part 1 • Hành động & Việc làm" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 1 • Đồ vật & Địa điểm" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 1 • Trạng thái & Vị trí" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 2 • Chủ điểm Người" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 2 • Chủ điểm Thời gian" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 2 • Chủ điểm Nơi chốn" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 2 • Chủ điểm Lý do" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 2 • Chủ điểm Cách làm" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 2 • Chủ điểm Lựa chọn" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 2 • Trả lời cảm thán" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 2 • Trả lời không biết" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 3 • Nơi làm việc 1" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 3 • Nơi làm việc 2" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 3 • Mua sắm & Di chuyển" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 3 • Nhà hàng, Y tế & Ngân hàng" to "Luyện Nghe Part 1–3 (TOEIC 450)",
    "Part 5-6 • Trọng tâm Phần 1 (1–100)" to "Luyện Đọc Part 5–6 (TOEIC 450)",
    "Part 5-6 • Trọng tâm Phần 2 (101–200)" to "Luyện Đọc Part 5–6 (TOEIC 450)",
    "Part 5-6 • Trọng tâm Phần 3 (201–300)" to "Luyện Đọc Part 5–6 (TOEIC 450)"
)

const val CUSTOM_TOPIC_CATEGORY = "Chủ đề tự tạo"

fun topicCategoryFor(topic: String): String = TopicCategoryMap[topic] ?: CUSTOM_TOPIC_CATEGORY
