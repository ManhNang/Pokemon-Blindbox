package com.pokemon.blindbox.service;

import com.pokemon.blindbox.dto.request.CreateWithdrawalRequest;
import com.pokemon.blindbox.dto.response.WithdrawalResponseDTO;
import com.pokemon.blindbox.entity.User;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.WithdrawalRequest;
import com.pokemon.blindbox.entity.enums.TransactionType;
import com.pokemon.blindbox.entity.enums.WithdrawalStatus;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.exception.ResourceNotFoundException;
import com.pokemon.blindbox.repository.UserRepository;
import com.pokemon.blindbox.repository.WalletRepository;
import com.pokemon.blindbox.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ Rút Xu về tài khoản ngân hàng (Mô hình Bán tự động — Admin duyệt & Quét VietQR):
 * - Tỷ giá quy đổi đồng bộ với Nạp Xu: 100 Xu = 1.000 ₫ (1 Xu = 10 ₫).
 * - Tối thiểu 5.000 Xu (50.000 ₫), bội số 100 Xu.
 * - Khi người dùng tạo yêu cầu: khóa bi quan Ví (SELECT ... FOR UPDATE), trừ ngay số Xu (chống double-spend).
 * - Admin duyệt (APPROVED): xác nhận đã chuyển khoản VietQR cho người dùng.
 * - Admin từ chối (REJECTED): tự động hoàn lại 100% số Xu vào ví người dùng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    public static final BigDecimal XU_TO_VND_RATE = new BigDecimal("10");
    public static final BigDecimal WITHDRAWAL_FEE_MULTIPLIER = new BigDecimal("0.90"); // Trừ 10% phí rút
    public static final BigDecimal MIN_WITHDRAWAL_XU = new BigDecimal("1000");
    public static final BigDecimal MAX_WITHDRAWAL_XU = new BigDecimal("10000000");

    private final WithdrawalRequestRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final UserRepository userRepository;

    @Transactional
    public Map<String, Object> createWithdrawal(Long userId, CreateWithdrawalRequest req) {
        BigDecimal xuAmount = req.xuAmount().setScale(0, RoundingMode.FLOOR).setScale(2, RoundingMode.HALF_UP);
        if (xuAmount.compareTo(MIN_WITHDRAWAL_XU) < 0) {
            throw new BusinessValidationException("Số Xu rút tối thiểu là 1.000 Xu!");
        }
        if (xuAmount.compareTo(MAX_WITHDRAWAL_XU) > 0) {
            throw new BusinessValidationException("Số Xu rút tối đa mỗi lần là 10.000.000 Xu!");
        }
        if (xuAmount.remainder(new BigDecimal("100")).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessValidationException("Số Xu rút phải là bội số của 100 Xu (VD: 1.000, 5.000, 10.000)!");
        }

        String bankCode = req.bankCode().trim().toUpperCase(Locale.ROOT);
        String accountNumber = req.accountNumber().trim();
        String accountName = normalizeAccountName(req.accountName());
        // Quy đổi 100 Xu = 1.000 ₫ và trừ 10% phí rút -> thực nhận = xuAmount * 10 * 0.90
        BigDecimal vndAmount = xuAmount
                .multiply(XU_TO_VND_RATE)
                .multiply(WITHDRAWAL_FEE_MULTIPLIER)
                .setScale(0, RoundingMode.FLOOR)
                .setScale(2, RoundingMode.HALF_UP);

        // Khóa bi quan ví để chống double-spend khi gửi 2 yêu cầu rút cùng lúc
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người dùng."));

        if (wallet.getBalance().compareTo(xuAmount) < 0) {
            throw new BusinessValidationException("Số dư khả dụng trong ví không đủ để thực hiện yêu cầu rút này!");
        }

        wallet.setBalance(wallet.getBalance().subtract(xuAmount));
        wallet = walletRepository.save(wallet);

        String code = "RUT" + userId + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);

        WithdrawalRequest saved = withdrawalRepository.save(WithdrawalRequest.builder()
                .userId(userId)
                .withdrawalCode(code)
                .xuAmount(xuAmount)
                .vndAmount(vndAmount)
                .bankCode(bankCode)
                .accountNumber(accountNumber)
                .accountName(accountName)
                .status(WithdrawalStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());

        // Lưu amount DƯƠNG theo đúng ràng buộc chk_wallet_tx_amount_positive (amount > 0)
        walletService.recordTransaction(
                wallet,
                TransactionType.WITHDRAWAL_REQUEST,
                xuAmount,
                String.format("Rút %s Xu (phí 10%%, thực nhận %s ₫) về %s - %s [%s]",
                        xuAmount.stripTrailingZeros().toPlainString(),
                        vndAmount.stripTrailingZeros().toPlainString(),
                        bankCode, accountNumber, code)
        );

        String username = userRepository.findById(userId).map(User::getUsername).orElse("Trainer #" + userId);
        log.info("User {} created withdrawal {} ({} Xu -> {} VND after 10% fee to {} {})",
                userId, code, xuAmount, vndAmount, bankCode, accountNumber);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("withdrawal", WithdrawalResponseDTO.fromEntity(saved, username));
        res.put("newWalletBalance", wallet.getBalance());
        res.put("message", String.format("Đã gửi yêu cầu rút %s Xu (phí 10%%, thực nhận %s ₫). Admin sẽ duyệt và chuyển khoản sớm nhất!",
                xuAmount.stripTrailingZeros().toPlainString(),
                vndAmount.stripTrailingZeros().toPlainString()));
        return res;
    }

    @Transactional(readOnly = true)
    public List<WithdrawalResponseDTO> getUserWithdrawals(Long userId) {
        String username = userRepository.findById(userId).map(User::getUsername).orElse("Trainer #" + userId);
        return withdrawalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(w -> WithdrawalResponseDTO.fromEntity(w, username))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminWithdrawals(String statusFilter) {
        List<WithdrawalRequest> list;
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter.trim())) {
            try {
                WithdrawalStatus st = WithdrawalStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
                list = withdrawalRepository.findByStatusOrderByCreatedAtDesc(st);
            } catch (IllegalArgumentException ex) {
                list = withdrawalRepository.findAllByOrderByCreatedAtDesc();
            }
        } else {
            list = withdrawalRepository.findAllByOrderByCreatedAtDesc();
        }

        List<Long> userIds = list.stream().map(WithdrawalRequest::getUserId).distinct().toList();
        Map<Long, String> usernameMap = userIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        List<WithdrawalResponseDTO> items = list.stream()
                .map(w -> WithdrawalResponseDTO.fromEntity(w, usernameMap.get(w.getUserId())))
                .toList();

        long pendingCount = withdrawalRepository.countByStatus(WithdrawalStatus.PENDING);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", items);
        res.put("pendingCount", pendingCount);
        return res;
    }

    @Transactional
    public Map<String, Object> approveWithdrawal(Long adminId, Long requestId, String note) {
        WithdrawalRequest wr = withdrawalRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu rút tiền #" + requestId));

        if (wr.getStatus() != WithdrawalStatus.PENDING) {
            throw new BusinessValidationException("Yêu cầu rút tiền này đã được xử lý trước đó (" + wr.getStatus() + ")!");
        }

        wr.setStatus(WithdrawalStatus.APPROVED);
        wr.setProcessedBy(adminId);
        wr.setProcessedAt(LocalDateTime.now());
        wr.setAdminNote(note != null && !note.isBlank() ? note.trim() : "Đã chuyển khoản VietQR thành công");
        wr = withdrawalRepository.save(wr);

        String username = userRepository.findById(wr.getUserId()).map(User::getUsername).orElse("Trainer #" + wr.getUserId());
        log.info("Admin {} APPROVED withdrawal {} ({} VND to {} {})",
                adminId, wr.getWithdrawalCode(), wr.getVndAmount(), wr.getBankCode(), wr.getAccountNumber());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("withdrawal", WithdrawalResponseDTO.fromEntity(wr, username));
        res.put("pendingCount", withdrawalRepository.countByStatus(WithdrawalStatus.PENDING));
        res.put("message", "Đã xác nhận chuyển tiền cho đơn " + wr.getWithdrawalCode() + "!");
        return res;
    }

    @Transactional
    public Map<String, Object> rejectWithdrawal(Long adminId, Long requestId, String reason) {
        WithdrawalRequest wr = withdrawalRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu rút tiền #" + requestId));

        if (wr.getStatus() != WithdrawalStatus.PENDING) {
            throw new BusinessValidationException("Yêu cầu rút tiền này đã được xử lý trước đó (" + wr.getStatus() + ")!");
        }

        String finalReason = (reason != null && !reason.isBlank())
                ? reason.trim()
                : "Thông tin tài khoản không hợp lệ hoặc bị hủy bởi Quản trị viên";

        // Hoàn lại 100% số Xu vào ví người dùng
        Long targetUserId = wr.getUserId();
        Wallet wallet = walletRepository.findByUserIdForUpdate(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví của người dùng #" + targetUserId));
        wallet.setBalance(wallet.getBalance().add(wr.getXuAmount()));
        wallet = walletRepository.save(wallet);

        walletService.recordTransaction(
                wallet,
                TransactionType.WITHDRAWAL_REFUND,
                wr.getXuAmount(),
                String.format("Hoàn %s Xu do từ chối yêu cầu rút [%s]: %s",
                        wr.getXuAmount().stripTrailingZeros().toPlainString(),
                        wr.getWithdrawalCode(),
                        finalReason)
        );

        wr.setStatus(WithdrawalStatus.REJECTED);
        wr.setProcessedBy(adminId);
        wr.setProcessedAt(LocalDateTime.now());
        wr.setAdminNote(finalReason);
        wr = withdrawalRepository.save(wr);

        String username = userRepository.findById(wr.getUserId()).map(User::getUsername).orElse("Trainer #" + wr.getUserId());
        log.info("Admin {} REJECTED withdrawal {} and refunded {} Xu to user {}",
                adminId, wr.getWithdrawalCode(), wr.getXuAmount(), wr.getUserId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("withdrawal", WithdrawalResponseDTO.fromEntity(wr, username));
        res.put("pendingCount", withdrawalRepository.countByStatus(WithdrawalStatus.PENDING));
        res.put("message", "Đã từ chối đơn " + wr.getWithdrawalCode() + " và hoàn lại " + wr.getXuAmount().stripTrailingZeros().toPlainString() + " Xu!");
        return res;
    }

    private String normalizeAccountName(String raw) {
        if (raw == null) return "";
        String noAccent = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return noAccent.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
