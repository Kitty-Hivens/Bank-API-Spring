package haru.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    @NotBlank(message = "Номер счета отправителя не может быть пустым")
    private String fromAccountNumber;

    @NotBlank(message = "Номер счета получателя не может быть пустым")
    private String toAccountNumber;

    @NotNull(message = "Сумма не может быть пустой")
    @Positive(message = "Сумма должна быть положительной")
    private BigDecimal amount;
}
