# Java-Multithreading
### **Task**
Parses given input files and determines if all the products of a given order have been shipped
to their respectve customer. If so, the program updates the status of the order in the output file.

### **Usage**
Program works only on UNIX based systems, run with the following arguments:
```
java Tema2 <folderPath> <numberOfThreads>
```
Where: 
- `<folderPath>` contains the needed input files: *orders.txt* and *order_products.txt*
- `<numberOfThreads>` specifies how many threads will be created

### **Design**
There are 2 main levels of multi-threding present separated into the following classes:
#### **ReaderThread**
Reads lines from *orders.txt* and creates tasks in the Thread Pool in order to search
for the specified products contained in the order.

When all the associated tasks have been solved, the ReaderThread checks if all the products
have been shipped. If yes, the status of the order is updated and a new order is read for processing.
#### **Thread Pool**
Awaits tasks to be created by the *ReaderThreads*. A tasks consists of parsing *order_products.txt* and
finding the specified *orderID* - *productID* pair. If found, the product is marked as shipped and the
answer is communicated back to the *ReaderThread* that created the task.

### **Implementation Details**
Seeing as all the Threads operate independently, synchronization is required only when writing to the
output files. This is solved trivially using simple mutexes when trying to write to the files.

*ReaderThreads* all use the same `BufferedReader` instance in order to share the same file descriptor
in order to read one after the other. Synchronisation is not required when reading as UNIX systems do
not allow multiple threads to read from files at the same time.

Threads in the work pool however each create their own `BufferedReader` instance in order to read
independently from each other.

The Thread Pool is created using `ExecutorService` found in the JDK.