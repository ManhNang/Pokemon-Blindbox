package com.pokemon.blindbox.dto.request;

import jakarta.validation.constraints.Size;

public record ProcessWithdrawalRequest(
        @Size(max = 255, message = "Ghi chú không vượt quá 255 ký tự!")
        String note
) {}
