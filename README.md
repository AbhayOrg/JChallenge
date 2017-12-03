# Creator : abhaykumar05@gmail.com


Few points to consider  : 
1.	The current account locking would only work if only single instance of application is running. For HA environment where multi-instances are need to be run, then distributed lock would be needed.
2.	Spring provides transaction framework for JPS repository. This feature of spring can be leveraged.
3.	Validation can be refactored and move to different class  from reusability and  separation-of-concern perspective. 
Two kind of validation can be done :
a.	Validation before actually making DB call ( e.g. checking if those two accounts are equal or not)
b.	Post DB validation ( e.g. whether account is active or not )
4.	Message should be standardized and should be moved to different class. Also Internal error codes should be defined and should be mapped to HTTP code.
5.	A unique ID can be prefixed in all the logs for a request, so that it would be easy to relate logs and debug while troubleshooting the issue when multiple request/response are happening concurrently.
6.	Notification can be done in Async as result of it does not impact success of transaction.
