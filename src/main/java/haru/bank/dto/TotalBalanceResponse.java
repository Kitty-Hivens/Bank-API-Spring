package haru.bank.dto;

import haru.bank.model.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TotalBalanceResponse {
    private Long userId;
    private BigDecimal totalBalanceUah;
    private Currency currency;
    private LocalDateTime calculatedAt;
}