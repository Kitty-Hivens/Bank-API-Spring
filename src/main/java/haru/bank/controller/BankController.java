package haru.bank.controller;

import haru.bank.dto.ConversionRequest;
import haru.bank.dto.DepositRequest;
import haru.bank.dto.TotalBalanceResponse;
import haru.bank.dto.TransferRequest;
import haru.bank.model.Account;
import haru.bank.model.Currency;
import haru.bank.model.Transaction;
import haru.bank.service.BankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/bank")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    /**
     * 1. Пополнение счета
     * POST /api/v1/bank/deposit
     */
    @PostMapping("/deposit")
    public ResponseEntity<Account> deposit(@Valid @RequestBody DepositRequest request) {
        Account account = bankService.deposit(request.getAccountNumber(), request.getAmount());
        return ResponseEntity.ok(account);
    }

    /**
     * 2. Перевод средств
     * POST /api/v1/bank/transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request) {
        Transaction tx = bankService.transfer(
            request.getFromAccountNumber(),
            request.getToAccountNumber(),
            request.getAmount()
        );
        return ResponseEntity.ok(tx);
    }

    /**
     * 3. Конвертация (в рамках одного юзера)
     * POST /api/v1/bank/users/{userId}/convert
     */
    @PostMapping("/users/{userId}/convert")
    public ResponseEntity<Transaction> convert(
            @PathVariable Long userId,
            @Valid @RequestBody ConversionRequest request) {
        
        Transaction tx = bankService.convert(
            userId,
            request.getFromAccountNumber(),
            request.getToAccountNumber(),
            request.getAmount()
        );
        return ResponseEntity.ok(tx);
    }

    /**
     * 4. Получение суммарного баланса в UAH
     * GET /api/v1/bank/users/{userId}/total-balance
     */
    @GetMapping("/users/{userId}/total-balance")
    public ResponseEntity<TotalBalanceResponse> getTotalBalance(@PathVariable Long userId) {
        BigDecimal total = bankService.getTotalBalanceInUah(userId);
        TotalBalanceResponse response = new TotalBalanceResponse(userId, total, Currency.UAH, LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}
