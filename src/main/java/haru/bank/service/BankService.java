package haru.bank.service;

import haru.bank.exception.OperationException;
import haru.bank.exception.ResourceNotFoundException;
import haru.bank.model.Account;
import haru.bank.model.Currency;
import haru.bank.model.Transaction;
import haru.bank.repository.AccountRepository;
import haru.bank.repository.ExchangeRateRepository;
import haru.bank.repository.TransactionRepository;
import haru.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final UserRepository userRepository; // Добавлен для проверки пользователя

    /**
     * 1. Пополнение счета в нужной валюте
     */
    @Transactional
    public Account deposit(String accountNumber, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperationException("Сумма пополнения должна быть положительной");
        }

        Account account = findAccountByNumber(accountNumber);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // Логируем транзакцию
        Transaction tx = new Transaction();
        tx.setType(Transaction.TransactionType.DEPOSIT);
        tx.setToAccount(account);
        tx.setAmount(amount);
        tx.setConvertedAmount(amount); // При пополнении суммы совпадают
        transactionRepository.save(tx);

        return account;
    }

    /**
     * 2. Перевод средств с одного счета на другой
     * (Обрабатывает как одновалютный, так и мультивалютный перевод)
     */
    @Transactional
    public Transaction transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperationException("Сумма перевода должна быть положительной");
        }
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new OperationException("Счет отправителя и получателя не могут совпадать");
        }

        Account fromAccount = findAccountByNumber(fromAccountNumber);
        Account toAccount = findAccountByNumber(toAccountNumber);

        // Проверка баланса
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new OperationException("Недостаточно средств на счете " + fromAccountNumber);
        }

        BigDecimal amountToReceive;
        BigDecimal rateUsed = null;

        if (fromAccount.getCurrency() == toAccount.getCurrency()) {
            // Одновалютный перевод
            amountToReceive = amount;
        } else {
            // Мультивалютный перевод (конвертация)
            BigDecimal rateFrom = getRateToUah(fromAccount.getCurrency());
            BigDecimal rateTo = getRateToUah(toAccount.getCurrency());
            rateUsed = rateFrom.divide(rateTo, 6, RoundingMode.HALF_UP);
            amountToReceive = amount.multiply(rateUsed).setScale(4, RoundingMode.HALF_UP);
        }

        // Выполняем перевод
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amountToReceive));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Логируем транзакцию
        Transaction tx = new Transaction();
        tx.setType(Transaction.TransactionType.TRANSFER);
        tx.setFromAccount(fromAccount);
        tx.setToAccount(toAccount);
        tx.setAmount(amount); // Сумма в валюте отправителя
        tx.setConvertedAmount(amountToReceive); // Сумма в валюте получателя
        tx.setRateUsed(rateUsed);
        return transactionRepository.save(tx);
    }

    /**
     * 3. Конвертация валюты по курсу в рамках счетов одного пользователя
     */
    @Transactional
    public Transaction convert(Long userId, String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
        // Проверяем, что пользователь существует
        userRepository.findById(userId).orElseThrow(() -> 
            new ResourceNotFoundException("Пользователь с ID " + userId + " не найден"));

        Account fromAccount = findAccountByNumber(fromAccountNumber);
        Account toAccount = findAccountByNumber(toAccountNumber);

        // Ключевая проверка: оба счета принадлежат одному пользователю
        if (!fromAccount.getUser().getId().equals(userId) || !toAccount.getUser().getId().equals(userId)) {
            throw new OperationException("Конвертация возможна только между счетами одного пользователя");
        }
        
        if (fromAccount.getCurrency() == toAccount.getCurrency()) {
             throw new OperationException("Нельзя конвертировать в ту же самую валюту");
        }

        // Логика идентична 'transfer', но тип транзакции другой
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OperationException("Сумма конвертации должна быть положительной");
        }
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new OperationException("Недостаточно средств на счете " + fromAccountNumber);
        }

        BigDecimal rateFrom = getRateToUah(fromAccount.getCurrency());
        BigDecimal rateTo = getRateToUah(toAccount.getCurrency());
        BigDecimal rateUsed = rateFrom.divide(rateTo, 6, RoundingMode.HALF_UP);
        BigDecimal amountToReceive = amount.multiply(rateUsed).setScale(4, RoundingMode.HALF_UP);

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amountToReceive));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction tx = new Transaction();
        tx.setType(Transaction.TransactionType.CONVERSION);
        tx.setFromAccount(fromAccount);
        tx.setToAccount(toAccount);
        tx.setAmount(amount);
        tx.setConvertedAmount(amountToReceive);
        tx.setRateUsed(rateUsed);
        return transactionRepository.save(tx);
    }

    /**
     * 4. Получение суммарных средств на счету одного пользователя в UAH
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceInUah(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Пользователь с ID " + userId + " не найден");
        }

        List<Account> userAccounts = accountRepository.findByUserId(userId);
        BigDecimal totalBalanceUah = BigDecimal.ZERO;

        for (Account account : userAccounts) {
            if (account.getCurrency() == Currency.UAH) {
                totalBalanceUah = totalBalanceUah.add(account.getBalance());
            } else {
                BigDecimal rate = getRateToUah(account.getCurrency());
                BigDecimal balanceInUah = account.getBalance().multiply(rate);
                totalBalanceUah = totalBalanceUah.add(balanceInUah);
            }
        }

        return totalBalanceUah.setScale(4, RoundingMode.HALF_UP);
    }

    // --- Вспомогательные методы ---

    private Account findAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Счет " + accountNumber + " не найден"));
    }

    private BigDecimal getRateToUah(Currency currency) {
        if (currency == Currency.UAH) {
            return BigDecimal.ONE;
        }
        return exchangeRateRepository.findByCurrency(currency)
                .orElseThrow(() -> new ResourceNotFoundException("Курс для " + currency + " не найден"))
                .getRateToUah();
    }
}