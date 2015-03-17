# Total-FIFO-ordered-multicast
Group messenger with Total And FIFO ordered multicast with a local persistent key-value storage. This application 
B-multicasts message to all other instances(including itself) and that message is stored in content provider as key-value
pair. All messages which will be delivered will follow Total and FIFO ordering i.e., all messages will have same key
(sequence number) in their respective content providers and message from each process will follow FIFO order.

All messages will have a unique key(starting from 0) and value being message itself will be stored in content 
provider of each of the applications.

It has been tested on 5 avd's(processes). It can be extended according to requirements.

