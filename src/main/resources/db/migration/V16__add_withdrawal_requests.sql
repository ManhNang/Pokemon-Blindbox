-- V16: Hệ thống Rút Xu về tài khoản ngân hàng (Bán tự động — Admin duyệt kèm VietQR)
CREATE TABLE IF NOT EXISTS withdrawal_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    withdrawal_code VARCHAR(32) NOT NULL,
    xu_amount DECIMAL(19, 2) NOT NULL,
    vnd_amount DECIMAL(19, 2) NOT NULL,
    bank_code VARCHAR(20) NOT NULL,
    account_number VARCHAR(40) NOT NULL,
    account_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_note VARCHAR(255) NULL,
    processed_by BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    CONSTRAINT uk_withdrawal_code UNIQUE (withdrawal_code),
    CONSTRAINT fk_withdrawal_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_withdrawal_user_created (user_id, created_at),
    INDEX idx_withdrawal_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
