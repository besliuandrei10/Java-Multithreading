import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.*;

class ReaderThread extends Thread {

    private Integer id;
    private BufferedReader reader;
    private ExecutorService tpe;

    ReaderThread(BufferedReader input, int id, ExecutorService tpe) {
        this.id = id;
        this.reader = input;
        this.tpe = tpe;
    }
    public void run() {
        try {
            String line = reader.readLine();
            while (line != null) {
                // Parse line for variables.
                String[] splitLine = line.split(",");
                String order = splitLine[0];
                int numberOfProducts = Integer.parseInt(splitLine[1]);

                // Check if order is empty, move on if so.
                if(numberOfProducts == 0) {
                    line = reader.readLine();
                    continue;
                }

                // Create Tasks in Task Pool.
                ArrayList<Future<Boolean>> list = new ArrayList<>();
                for(int i = 1; i <= numberOfProducts; i++) {
                    list.add(tpe.submit(new Task(order, i)));
                }

                // Presume Order has been shipped, check shipped products.
                boolean status = true;
                for (Future<Boolean> element : list) {
                    if (!element.get()) {
                        status = false;
                    }
                }

                // Await answer and write to file.
                if(status) {
                    synchronized (Tema2.readerMutex) {
                        Tema2.output.write(line + ",shipped\n");
                    }
                }

                // Read Next Line
                line = reader.readLine();
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}

class Task implements Callable {

    private final String order;
    private final int productNumber;

    Task(String order, int productNumber) {
        this.order = order;
        this.productNumber = productNumber;
    }
    @Override
    public Boolean call() throws IOException {
        // Open products file for reading.
        String readPath = Tema2.folderPath + "/order_products.txt";
        BufferedReader read = new BufferedReader(new FileReader(readPath));

        int count = 0;
        String line = read.readLine();
        while (line != null) {
            // Split line in order to extract variables.
            String[] splitLine = line.split(",");

            if (order.equals(splitLine[0])) count++;
            // If we found the specified product, write to file and exit.
            if (count == productNumber) {
                synchronized (Tema2.writerMutex) {
                    Tema2.outputProducts.write(line + ",shipped\n");
                }
                return true;
            }
            line = read.readLine();
        }

        return false;
    }
}
public class Tema2 {

    public static final Object readerMutex = 0;
    public static final Object writerMutex = 0;
    public static String folderPath;
    public static BufferedWriter outputProducts;
    public static BufferedWriter output;
    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length != 2) {
            System.out.println("Incorrect number of arguments! <folderPath> <Thread Count>");
            return;
        }

        // Folder Path visible for all the other Threads.
        folderPath = args[0];

        // Input Files and Reader
        File input = new File(folderPath + "/orders.txt");
        BufferedReader reader = new BufferedReader(new FileReader(input));

        // Output Files
        output = new BufferedWriter(new FileWriter("orders_out.txt"));
        outputProducts = new BufferedWriter(new FileWriter("order_products_out.txt"));

        // Number of Threads
        int P = Integer.parseInt(args[1]);

        // Store and Init WorkerPool
        ExecutorService tpe = Executors.newFixedThreadPool(P);

        // Store and init ReaderThreads.
        Thread[] threads = new Thread[P];
        for(int i = 0; i < P; i++) {
            ReaderThread thread = new ReaderThread(reader, i, tpe);
            threads[i] = thread;
            thread.start();
        }

        // Join Reader Threads
        for(int i = 0; i < P; i++) {
            threads[i].join();
        }

        // Close Worker Pool
        tpe.shutdown();

        // Close files.
        output.close();
        outputProducts.close();
    }
}
