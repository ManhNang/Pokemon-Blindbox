package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateWithdrawalRequest(
        @NotNull(message = "Vui lòng nhập số Xu muốn rút!")
        @DecimalMin(value = "1000", message = "Số Xu rút tối thiểu là 1.000 Xu!")
        @DecimalMax(value = "10000000", message = "Số Xu rút tối đa mỗi lần là 10.000.000 Xu!")
        BigDecimal xuAmount,

        @NotBlank(message = "Vui lòng chọn ngân hàng nhận tiền!")
        @Size(max = 20, message = "Mã ngân hàng không hợp lệ!")
        String bankCode,

        @NotBlank(message = "Vui lòng nhập số tài khoản ngân hàng!")
        @Pattern(regexp = "^[0-9A-Za-z]{6,30}$", message = "Số tài khoản chỉ gồm chữ/số (6–30 ký tự, không khoảng trắng)!")
        String accountNumber,

        @NotBlank(message = "Vui lòng nhập tên chủ tài khoản!")
        @Size(min = 2, max = 120, message = "Tên chủ tài khoản phải từ 2 đến 120 ký tự!")
        String accountName
) {}
