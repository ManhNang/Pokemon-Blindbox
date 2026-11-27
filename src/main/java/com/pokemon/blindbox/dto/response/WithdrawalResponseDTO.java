package com.pokemon.blindbox.dto.response;

import com.pokemon.blindbox.entity.WithdrawalRequest;
import com.pokemon.blindbox.entity.enums.WithdrawalStatus;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public record WithdrawalResponseDTO(
        Long id,
        Long userId,
        String username,
        String withdrawalCode,
        BigDecimal xuAmount,
        BigDecimal vndAmount,
        String bankCode,
        String accountNumber,
        String accountName,
        WithdrawalStatus status,
        String adminNote,
        String vietQrUrl,
        LocalDateTime createdAt,
        LocalDateTime processedAt
) {
    public static WithdrawalResponseDTO fromEntity(WithdrawalRequest entity, String username) {
        long vndLong = entity.getVndAmount() != null ? entity.getVndAmount().longValue() : 0L;
        String encodedInfo = URLEncoder.encode(entity.getWithdrawalCode(), StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(entity.getAccountName(), StandardCharsets.UTF_8);
        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                entity.getBankCode(),
                entity.getAccountNumber(),
                vndLong,
                encodedInfo,
                encodedName
        );
        return new WithdrawalResponseDTO(
                entity.getId(),
                entity.getUserId(),
                username != null ? username : ("Trainer #" + entity.getUserId()),
                entity.getWithdrawalCode(),
                entity.getXuAmount(),
                entity.getVndAmount(),
                entity.getBankCode(),
                entity.getAccountNumber(),
                entity.getAccountName(),
                entity.getStatus(),
                entity.getAdminNote(),
                qrUrl,
                entity.getCreatedAt(),
                entity.getProcessedAt()
        );
    }
}
