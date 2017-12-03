package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.exception.InvalidAccountException;
import com.db.awmd.challenge.exception.InvalidAmountException;
import com.db.awmd.challenge.exception.TransferException;
import com.db.awmd.challenge.exception.TransferFailedException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.db.awmd.challenge.service.AccountsService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  private AccountsService acntServiceSpied;
  
  @Mock
  private AccountsRepository accountRepo;
  
  @Before
  public void init(){
	  acntServiceSpied = Mockito.spy(accountsService);
	 Mockito.when(acntServiceSpied.getAccountsRepository()).thenReturn(accountRepo);
  }
  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
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
  public void transferAmount() throws Exception {
	  String account1 = "Id1-" + UUID.randomUUID().toString();
	  String account2 = "Id2-" + UUID.randomUUID().toString();
	  
	  this.accountsService.createAccount(new Account(account1, new BigDecimal("100.0")));
	  this.accountsService.createAccount(new Account(account2, new BigDecimal("100.0")));
	  this.accountsService.transferAmount(account1,account2,new BigDecimal("10.0"));
	  assertThat(this.accountsService.getAccount(account1).getBalance()).isEqualTo("90.0");
	  assertThat(this.accountsService.getAccount(account2).getBalance()).isEqualTo("110.0");
  }
  
  @Test(expected=InvalidAmountException.class)
  public void transferAmount_failOnNegativeAmount() throws Exception {
	  String account1 = "Id1-" + UUID.randomUUID().toString();
	  String account2 = "Id2-" + UUID.randomUUID().toString();
	  
	  this.accountsService.createAccount(new Account(account1, new BigDecimal("100.0")));
	  this.accountsService.createAccount(new Account(account2, new BigDecimal("100.0")));
	  this.accountsService.transferAmount(account1,account2,new BigDecimal("-1"));
  }
  
  @Test(expected=InvalidAmountException.class)
  public void transferAmount_failOnZeroAmount() throws Exception {
	  String account1 = "Id1-" + UUID.randomUUID().toString();
	  String account2 = "Id2-" + UUID.randomUUID().toString();
	  
	  this.accountsService.createAccount(new Account(account1, new BigDecimal("100.0")));
	  this.accountsService.createAccount(new Account(account2, new BigDecimal("100.0")));
	  this.accountsService.transferAmount(account1,account2,new BigDecimal("0"));
  }
  
  @Test(expected=InvalidAccountException.class)
  public void transferAmount_failOnNonExistentFromAccount() throws Exception {
	  String account1 = "Id1-" + UUID.randomUUID().toString();
	  
	  this.accountsService.createAccount(new Account(account1, new BigDecimal("100.0")));
	  this.accountsService.transferAmount(String.valueOf(System.currentTimeMillis()),account1,new BigDecimal("10"));
  }
  
  @Test(expected=InvalidAccountException.class)
  public void transferAmount_failOnNonExistentToAccount() throws Exception {
	  String account1 = "Id1-" + UUID.randomUUID().toString();
	  
	  this.accountsService.createAccount(new Account(account1, new BigDecimal("100.0")));
	  this.accountsService.transferAmount(account1,String.valueOf(System.currentTimeMillis()),new BigDecimal("10"));
  }
  
  @Test(expected=TransferException.class)
  public void transferAmount_failOnTransferWithinSameAccount() throws Exception {
	  String account1 = "Id1-" + UUID.randomUUID().toString();
	  
	  this.accountsService.createAccount(new Account(account1, new BigDecimal("100.0")));
	  this.accountsService.transferAmount(account1,account1,new BigDecimal("10"));
  }
  
  @Test(expected=InsufficientBalanceException.class)
  public void transferAmount_failOnInsufficientBalance() throws Exception {
	  String account1 = "Id1-" + UUID.randomUUID().toString();
	  String account2 = "Id2-" + UUID.randomUUID().toString();
	  
	  this.accountsService.createAccount(new Account(account1));
	  this.accountsService.createAccount(new Account(account2));
	  this.accountsService.transferAmount(account1,account2,new BigDecimal("110.0"));
  }
  
  @Test(expected=TransferFailedException.class)
  public void transferAmount_FailedOnDebit() throws Exception {
	  String id1 = "Id1-" + UUID.randomUUID().toString();
	  String id2 = "Id2-" + UUID.randomUUID().toString();

	  Account acnt1 = new Account(id1, new BigDecimal("100"));
	  Account acnt2 = new Account(id2, new BigDecimal("100"));
	  Mockito.when(accountRepo.getAccount(id1)).thenReturn(acnt1);
	  Mockito.when(accountRepo.getAccount(id2)).thenReturn(acnt2);
	  Mockito.doThrow(new RuntimeException()).when(accountRepo).debitAccount(anyString(), anyObject());
	  this.acntServiceSpied.transferAmount(id1,id2,new BigDecimal("10.0"));
  }
  
  @Test(expected=TransferFailedException.class)
  public void transferAmount_FailedOnCredit() throws Exception {
	  String id1 = "Id1-" + UUID.randomUUID().toString();
	  String id2 = "Id2-" + UUID.randomUUID().toString();

	  Account acnt1 = new Account(id1, new BigDecimal("100"));
	  Account acnt2 = new Account(id2, new BigDecimal("100"));
	  Mockito.when(accountRepo.getAccount(id1)).thenReturn(acnt1);
	  Mockito.when(accountRepo.getAccount(id2)).thenReturn(acnt2);
	  Mockito.doThrow(new RuntimeException()).when(accountRepo).creditAccount(anyString(), anyObject());
	  this.acntServiceSpied.transferAmount(id1,id2,new BigDecimal("10.0"));
  }
  
  @Test
  public void transferAmount_CheckRollbackOnFailure() throws Exception {
	  String id1 = "Id1-" + UUID.randomUUID().toString();
	  String id2 = "Id2-" + UUID.randomUUID().toString();

	  Account acnt1 = new Account(id1, new BigDecimal("100"));
	  Account acnt2 = new Account(id2, new BigDecimal("100"));
	  acntServiceSpied.createAccount(acnt1);
	  acntServiceSpied.createAccount(acnt2);
	  Mockito.when(accountRepo.getAccount(id1)).thenReturn(acnt1);
	  Mockito.when(accountRepo.getAccount(id2)).thenReturn(acnt2);
	  Mockito.doThrow(new RuntimeException()).when(accountRepo).creditAccount(anyString(), anyObject());
	  try{
		  this.acntServiceSpied.transferAmount(id1,id2,new BigDecimal("10.0"));
	  }catch(TransferFailedException tfe){
		  //expected
	  }
	  
	  acnt1 = acntServiceSpied.getAccount(id1);
	  assertThat(acnt1.getBalance()).isEqualTo("100");
	  acnt2 = acntServiceSpied.getAccount(id2);
	  assertThat(acnt2.getBalance()).isEqualTo("100");
  }
}
