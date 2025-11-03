package haru.bank.config;

import haru.bank.model.Account;
import haru.bank.model.Currency;
import haru.bank.model.ExchangeRate;
import haru.bank.model.User;
import haru.bank.repository.AccountRepository;
import haru.bank.repository.ExchangeRateRepository;
import haru.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Этот класс создает тестовые данные при запуске приложения:
 * 1. Курсы валют
 * 2. Двух пользователей
 * 3. По 3 счета (USD, EUR, UAH) для каждого пользователя
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ExchangeRateRepository rateRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        // 1. Создаем курсы валют (если их нет)
        if (rateRepository.count() == 0) {
            rateRepository.save(new ExchangeRate(Currency.USD, new BigDecimal("39.50")));
            rateRepository.save(new ExchangeRate(Currency.EUR, new BigDecimal("42.10")));
            rateRepository.save(new ExchangeRate(Currency.UAH, BigDecimal.ONE));
        }

        // 2. Создаем пользователей и счета (если их нет)
        if (userRepository.count() == 0) {
            // Пользователь 1
            User user1 = new User();
            user1.setUsername("user_one");
            user1.setPassword("pass123");
            userRepository.save(user1);
            
            createAccount(user1, Currency.UAH, new BigDecimal("10000.00"));
            createAccount(user1, Currency.USD, new BigDecimal("500.00"));
            createAccount(user1, Currency.EUR, new BigDecimal("300.00"));

            // Пользователь 2
            User user2 = new User();
            user2.setUsername("user_two");
            user2.setPassword("pass456");
            userRepository.save(user2);

            createAccount(user2, Currency.UAH, new BigDecimal("5000.00"));
            createAccount(user2, Currency.USD, new BigDecimal("100.00"));
            createAccount(user2, Currency.EUR, new BigDecimal("50.00"));
        }
    }

    private void createAccount(User user, Currency currency, BigDecimal balance) {
        Account account = new Account();
        account.setUser(user);
        account.setCurrency(currency);
        account.setBalance(balance);
        // accountNumber сгенерируется в @PrePersist
        accountRepository.save(account);
    }
}