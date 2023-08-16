package com.dws.challenge;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transfer;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.OverdraftException;
import com.dws.challenge.exception.SameAccountTransferException;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @MockBean
  private NotificationService notificationService;

  private Account account1;
  private Account account2;

  @BeforeEach
  public void setUp() {
    account1 = new Account("1", new BigDecimal(1000));
    account2 = new Account("2", new BigDecimal(0));
    this.accountsService.createAccount(account1);
    this.accountsService.createAccount(account2);
  }

  @AfterEach
  public void cleanUp() {
    this.accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  @Test
  void transfer_failOnOverdraft(){
    Transfer transfer = Transfer.builder()
      .toAccountId("1")
      .fromAccountId("2")
      .amount(new BigDecimal(1000))
      .build();
    assertThrows(OverdraftException.class, () -> this.accountsService.transfer(transfer));
  }

  @Test
  void transfer_concurrentTransfers() throws InterruptedException {
    int numThreads = 10;
    Transfer transfer1 = Transfer.builder()
      .toAccountId("2")
      .fromAccountId("1")
      .amount(new BigDecimal(100))
      .build();
    Transfer transfer2 = Transfer.builder()
      .toAccountId("1")
      .fromAccountId("2")
      .amount(new BigDecimal(1))
      .build();
    ExecutorService service = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    for (int i = 0; i < numThreads; i++) {
      int finalI = i;
      service.submit(() -> {
        if (finalI % 2 == 0) {
          this.accountsService.transfer(transfer1);
        } else {
          this.accountsService.transfer(transfer2);
        }
        latch.countDown();
      });
    }
    latch.await();
    assertEquals(new BigDecimal(505), this.account1.getBalance());
    assertEquals(new BigDecimal(495), this.account2.getBalance());
    verify(this.notificationService, times(numThreads * 2))
      .notifyAboutTransfer(any(Account.class), anyString());

  }

  @Test
  void transfer_failsBetweenSameAccount(){
    Transfer transfer = Transfer.builder()
      .toAccountId("1")
      .fromAccountId("1")
      .amount(new BigDecimal(1000))
      .build();
    assertThrows(SameAccountTransferException.class, () -> this.accountsService.transfer(transfer));
  }

  @Test
  void getAccount(){
    Account account = this.accountsService.getAccount("1");
    assertEquals(account, this.account1);
  }

  @Test
  void getAccount_failsOnNonExistingId(){
    assertThrows(AccountNotFoundException.class, () -> this.accountsService.getAccount("3"));
  }


}
