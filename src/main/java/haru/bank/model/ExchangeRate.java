package haru.bank.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сущность "Курс валют".
 * Хранит курс *к* UAH.
 * Например, для USD: rateToUah = 39.5 (означает 1 USD = 39.5 UAH)
 * Для UAH: rateToUah = 1.0
 */
@Entity
@Table(name = "exchange_rates")
@Data
@NoArgsConstructor
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rateToUah; // Сколько UAH за 1 единицу 'currency'

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    public ExchangeRate(Currency currency, BigDecimal rateToUah) {
        this.currency = currency;
        this.rateToUah = rateToUah;
    }
}
