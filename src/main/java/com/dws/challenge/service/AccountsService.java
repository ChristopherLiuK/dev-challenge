package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transfer;
import com.dws.challenge.exception.OverdraftException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Getter
@Service
@RequiredArgsConstructor
public class AccountsService {

  private final AccountsRepository accountsRepository;

  private final NotificationService notificationService;

  private static final Object tieLock = new Object();

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transfer(Transfer transfer) throws OverdraftException{
    Account fromAccount = this.getAccount(transfer.getFromAccountId());
    Account toAccount = this.getAccount(transfer.getToAccountId());

    if (isOverdraft(fromAccount, transfer.getAmount())) {
      throw new OverdraftException("Insufficient funds");
    }

    int fromHash = System.identityHashCode(fromAccount);
    int toHash = System.identityHashCode(toAccount);

    if (fromHash < toHash) {
      synchronized (fromAccount) {
        synchronized (toAccount) {
          updateBalance(fromAccount, toAccount, transfer.getAmount());
        }
      }
    } else if (fromHash > toHash) {
      synchronized (toAccount) {
        synchronized (fromAccount) {
          updateBalance(fromAccount, toAccount, transfer.getAmount());
        }
      }
    } else {
      synchronized (tieLock) {
        synchronized (fromAccount) {
          synchronized (toAccount) {
            updateBalance(fromAccount, toAccount, transfer.getAmount());
          }
        }
      }
    }
  }

  private void updateBalance(Account fromAccount, Account toAccount, BigDecimal amount) {
    fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
    toAccount.setBalance(toAccount.getBalance().add(amount));
    this.accountsRepository.updateAccount(fromAccount);
    this.accountsRepository.updateAccount(toAccount);
    notifyUsers(fromAccount, toAccount, amount);
  }

  private Boolean isOverdraft(Account account, BigDecimal amount) {
    return account.getBalance().compareTo(amount) < 0;
  }

  private void notifyUsers(Account fromAccount, Account toAccount, BigDecimal amount) {
    notificationService.notifyAboutTransfer(fromAccount,
      "Transfer to account " + toAccount.getAccountId() + " completed successfully. Amount: " + amount);
    notificationService.notifyAboutTransfer(toAccount,
      "Transfer from account " + fromAccount.getAccountId() + " completed successfully. Amount: " + amount);
  }

}
