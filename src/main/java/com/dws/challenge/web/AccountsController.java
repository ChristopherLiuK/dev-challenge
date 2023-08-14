package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transfer;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.OverdraftException;
import com.dws.challenge.exception.SameAccountTransferException;
import com.dws.challenge.service.AccountsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountsController {

  private final AccountsService accountsService;


  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);

    try {
    this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public ResponseEntity<Object> getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    try {
      return new ResponseEntity<>(this.accountsService.getAccount(accountId), HttpStatus.OK);
    } catch (AccountNotFoundException anfe) {
      return new ResponseEntity<>(anfe.getMessage(), HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/transfer")
  public ResponseEntity<Object> transfer(@RequestBody @Valid Transfer transfer) {
    log.info("Transfering {} from {} to {}", transfer.getAmount(), transfer.getFromAccountId(), transfer.getToAccountId());
    try {
      this.accountsService.transfer(transfer);
    } catch (OverdraftException | SameAccountTransferException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (AccountNotFoundException anfe) {
      return new ResponseEntity<>(anfe.getMessage(), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

}
