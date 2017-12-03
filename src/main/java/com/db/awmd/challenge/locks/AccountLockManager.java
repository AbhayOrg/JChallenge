package com.db.awmd.challenge.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 
 * @author Abhay Kumar
 *
 */
@Component
public class AccountLockManager implements LockManager<String>{

	/*
	 * Assumption here is, there can be max Integer.MAX_VALUE different accounts
	 * whose transactions can happen in parallel.
	 */
	Map<String, AccountLock> accountLockMap = new HashMap<>();
	
	@Override
	public synchronized Lock getLock(String accountId) {
		AccountLock accountLock = accountLockMap.get(accountId);
		if(accountLock == null){
			accountLock = new AccountLock();
			accountLock.setLock(new ReentrantLock(true));
			accountLockMap.put(accountId,accountLock);
		}
		accountLock.setLockCount(accountLock.getLockCount()+1);
		return accountLock.getLock();
	}

	@Override
	public synchronized void informLockRelease(String accountId) {
		AccountLock accountLock = accountLockMap.get(accountId);
		if(accountLock != null){
			accountLock.setLockCount(accountLock.getLockCount()-1);
			if(accountLock.getLockCount()<=0){
				accountLockMap.remove(accountId);
			}
		}
		
	}

	@Data
	class AccountLock{
		private Lock lock;
		private int lockCount;
	}
}
