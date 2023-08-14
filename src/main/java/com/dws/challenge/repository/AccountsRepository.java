package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;

import java.util.Optional;

public interface AccountsRepository {

  void createAccount(Account account) throws DuplicateAccountIdException;

  Optional<Account> getAccount(String accountId);

  void clearAccounts();

  void updateAccount(Account account);
}
