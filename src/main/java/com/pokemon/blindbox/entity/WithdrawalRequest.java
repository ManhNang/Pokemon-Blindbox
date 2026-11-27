package com.pokemon.blindbox.entity;

import com.pokemon.blindbox.entity.enums.WithdrawalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawal_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "withdrawal_code", nullable = false, unique = true, length = 32)
    private String withdrawalCode;

    @Column(name = "xu_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal xuAmount;

    @Column(name = "vnd_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal vndAmount;

    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Column(name = "account_number", nullable = false, length = 40)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 120)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Column(name = "admin_note", length = 255)
    private String adminNote;

    @Column(name = "processed_by")
    private Long processedBy;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = WithdrawalStatus.PENDING;
        }
    }
}
