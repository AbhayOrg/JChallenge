package com.db.awmd.challenge.locks;

import java.util.concurrent.locks.Lock;

/**
 * 
 * @author Abhay Kumar
 *
 * @param <T>
 */
public interface LockManager<T> {

	Lock getLock(T obj);
	void informLockRelease(T obj);
}
