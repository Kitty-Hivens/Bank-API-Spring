package haru.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DepositRequest {
    @NotBlank(message = "Номер счета не может быть пустым")
    private String accountNumber;

    @NotNull(message = "Сумма не может быть пустой")
    @Positive(message = "Сумма должна быть положительной")
    private BigDecimal amount;
}