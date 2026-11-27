package com.pokemon.blindbox.controller;

import com.pokemon.blindbox.dto.request.CreateWithdrawalRequest;
import com.pokemon.blindbox.dto.response.WithdrawalResponseDTO;
import com.pokemon.blindbox.security.AuthenticatedUserProvider;
import com.pokemon.blindbox.service.WithdrawalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/withdrawals")
@RequiredArgsConstructor
@Tag(name = "4C. Withdrawal", description = "Yêu cầu rút Xu về tài khoản ngân hàng (Admin duyệt kèm VietQR)")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final AuthenticatedUserProvider authUserProvider;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createWithdrawal(
            @Valid @RequestBody CreateWithdrawalRequest request
    ) {
        Long userId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(withdrawalService.createWithdrawal(userId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<WithdrawalResponseDTO>> getMyWithdrawals() {
        Long userId = authUserProvider.getRequiredUserId();
        return ResponseEntity.ok(withdrawalService.getUserWithdrawals(userId));
    }
}
