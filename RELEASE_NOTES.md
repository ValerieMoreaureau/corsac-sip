# Jain-SIP-RI Release Notes

### Tags

The folowing tags are used to categorize and state the scope of a change

* **security improvement** tags changes related to security
* **commercial** tags changes that are available only in the commercial RestcommOne product

# JSIP-RI Release Notes

## 7.1.1 version 2019-06-26

### Release Unit/Integration Tests
https://cxs.restcomm.com/job/TelScale-JAIN-SIP-7-MultiPipeline/job/ts2/16/

### New features
* N/A

### Breaking Changes

* N/A

### Bug fixes

* BS-2678: Thread-safe transaction data
    * Transaction data is now powered by a concurrent map to make it thread-safe.


## 7.1.0 version 2019-05-30

### Release Unit/Integration Tests
https://cxs.restcomm.com/job/TelScale-JAIN-SIP-7-MultiPipeline/job/ts2/14/

### New features
* **BS-2213:**
    * Transaction application data as key-value data store.
    * Added sub-types of ServerResponseInterface: TransactionResponseInterface and DialogResponseInterface to allow 
    fine-grained operations. 

### Breaking Changes

* N/A

### Bug fixes

* Removed SCTP module from default Maven profile