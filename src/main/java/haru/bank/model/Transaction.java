package haru.bank.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность "Транзакция"
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum TransactionType {
        DEPOSIT, // Пополнение
        TRANSFER, // Перевод (между разными счетами, возможно, разными валютами)
        CONVERSION // Конвертация (в рамках счетов одного юзера)
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id") // null для DEPOSIT
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id") // не null
    private Account toAccount;

    // Сумма в валюте счета 'fromAccount'.
    // Для DEPOSIT - это сумма в валюте 'toAccount'.
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // Сумма, зачисленная на 'toAccount' (актуально для конвертации/перевода)
    @Column(precision = 19, scale = 4)
    private BigDecimal convertedAmount;

    // Использованный курс (From -> To)
    @Column(precision = 19, scale = 6)
    private BigDecimal rateUsed;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}