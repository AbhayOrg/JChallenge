package com.db.awmd.challenge.service;

import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.MoneyTransferRequest;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.exception.InvalidAccountException;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.exception.TransferException;
import com.db.awmd.challenge.exception.TransferFailedException;
import com.db.awmd.challenge.locks.AccountLockManager;
import com.db.awmd.challenge.repository.AccountsRepository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  
  @Getter
  private final AccountLockManager accountLockManager;

  @Getter
  private final NotificationService notificationService;
  
  @Autowired
  public AccountsService(AccountsRepository accountsRepository, AccountLockManager accountLockManager, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.accountLockManager = accountLockManager;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }
  
//  @Transactional
  public void transferAmount(String from, String to, BigDecimal amount){
		/*
		 * Transaction behaviour can be achieved by making use
		 * of @Transactional annotation and providing TransactionManager with
		 * required JPA repository.
		 * 
		 * The current AccountsRepository is hashMap backed which i think is for
		 * testing purpose, real database repository might be oracle or others. And thus i have
		 * reverted the transaction manually instead.
		 */
	  
	  Lock firstLock = null;
	  Lock secondLock = null;
	  // Order needs to be maintained to avoid deadlock 
	  if(from.compareTo(to)<0){
		  firstLock = getAccountLockManager().getLock(from);
		  secondLock = getAccountLockManager().getLock(to);
	  }else{
		  firstLock = getAccountLockManager().getLock(to);
		  secondLock = getAccountLockManager().getLock(from);
	  }
	  
	  try{
		  firstLock.lock();
		  secondLock.lock();
		  Account fromAccount = getAccountsRepository().getAccount(from );
		  Account toAccount = getAccountsRepository().getAccount(to );
		  validate(fromAccount, toAccount, amount);
		  
		  log.debug("Debiting {} from account {}.", amount, from);
		  getAccountsRepository().debitAccount(from,amount);
		  log.debug("Successfully debited {} from account {}.", amount, from);
		  try{
			  log.debug("Crediting {} to account {}.", amount, to);
			  getAccountsRepository().creditAccount(to,amount);
			  log.debug("Successfully credited {} to account {}.", amount, to);
		  }catch(Exception e){
			  //revert 
			  log.error("Error while crediting {} to account {}, crediting back {} to {}", amount,to,amount,from,e);
			  try{
				  getAccountsRepository().creditAccount(from,amount); 
				  log.debug("Successfully credited {} to account {}.", from, amount );
			  }catch(Exception ine){
				  log.error("Error while reverting transaction between account {} and {}. Please contact support.",from, to,ine);
				  throw new TransferFailedException("Transfer failed due to internal error.");
			  }
			  throw new TransferFailedException("Transfer failed due to internal error.");
		  }
		  sendNotification(fromAccount,toAccount,amount);
	  }catch(InvalidAccountException iac){
		  log.info("Invalid account specified.");
		  throw iac;
	  }catch(TransferException te){
		  log.info("Validation failed for transfer of {} from account {} to account {}.",amount, from, to);
		  throw te;
	  }catch(Exception e ){
		  log.error("Error while tansferring from money {} from {} to {}.", amount, from, to, e);
		  throw new TransferFailedException("Transfer failed due to internal error");
	  }finally{
		  secondLock.unlock();
		  firstLock.unlock();
		  getAccountLockManager().informLockRelease(from);
		  getAccountLockManager().informLockRelease(to);
	  }

  }

	private void sendNotification(Account from, Account to, BigDecimal amount) {
		getNotificationService().notifyAboutTransfer(from, "Transferred " + amount + " to account "+ to.getAccountId());
		getNotificationService().notifyAboutTransfer(to, "Received "+  amount + " from account " + from.getAccountId());
	}

	private void validate(Account fromAccount, Account toAccount, BigDecimal amount) throws TransferException {
		// In future if this validation would grow ( or reuse of this needed ),
		// we can move this validation
		// to different class by defining a generic framework of validation
		
		
		if(fromAccount == null || toAccount == null){
			throw new InvalidAccountException("Invalid account.");
		}
		if(fromAccount.getAccountId().equals(toAccount.getAccountId())){
			throw new TransferException("Transfer cannot be done within same account.");
		}
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidAmountException("Positive amount needed for valid transaction.");
		}
		if (fromAccount.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
			throw new InsufficientBalanceException(
					"Account " + fromAccount.getAccountId() + " does not have enough balance.");
		}
	}
}
