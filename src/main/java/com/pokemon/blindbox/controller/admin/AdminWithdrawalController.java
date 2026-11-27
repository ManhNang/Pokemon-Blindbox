package com.pokemon.blindbox.controller.admin;

import com.pokemon.blindbox.dto.request.ProcessWithdrawalRequest;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.WithdrawalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/withdrawals")
@RequiredArgsConstructor
@Tag(name = "1C. Admin Withdrawals", description = "Quản trị & duyệt yêu cầu rút tiền ngân hàng (VietQR Payout)")
public class AdminWithdrawalController {

    private final WithdrawalService withdrawalService;
    private final AuthenticatedUserProvider authUserProvider;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listWithdrawals(
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status
    ) {
        return ResponseEntity.ok(withdrawalService.getAdminWithdrawals(status));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveWithdrawal(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ProcessWithdrawalRequest request
    ) {
        Long adminId = authUserProvider.getRequiredUserId();
        String note = request != null ? request.note() : null;
        return ResponseEntity.ok(withdrawalService.approveWithdrawal(adminId, id, note));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectWithdrawal(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ProcessWithdrawalRequest request
    ) {
        Long adminId = authUserProvider.getRequiredUserId();
        String reason = request != null ? request.note() : null;
        return ResponseEntity.ok(withdrawalService.rejectWithdrawal(adminId, id, reason));
    }
}
