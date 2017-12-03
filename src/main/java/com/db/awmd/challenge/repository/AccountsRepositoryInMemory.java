package com.db.awmd.challenge.repository;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

  private final Map<String, Account> accounts = new ConcurrentHashMap<>();

  @Override
  public void createAccount(Account account) throws DuplicateAccountIdException {
    Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
    if (previousAccount != null) {
      throw new DuplicateAccountIdException(
        "Account id " + account.getAccountId() + " already exists!");
    }
  }

  @Override
  public Account getAccount(String accountId) {
    return accounts.get(accountId);
  }

  @Override
  public void clearAccounts() {
    accounts.clear();
  }

  @Override
  public void debitAccount(String accountId, BigDecimal amount) {
	  Account  account = accounts.get(accountId);
	  BigDecimal balance = account.getBalance();
	  balance = balance.subtract(amount);
	  account.setBalance(balance);
  }

  @Override
  public void creditAccount(String accountId, BigDecimal amount) {
	  Account  account = accounts.get(accountId);
	  BigDecimal balance = account.getBalance();
	  balance = balance.add(amount);
	  account.setBalance(balance);
  }
  
}
