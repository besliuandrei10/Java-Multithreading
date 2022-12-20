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
                System.out.println("Thread " + id + " processing order " + line);

                // Send Tasks to Pool
                String[] splitLine = line.split(",");
                String order = splitLine[0];
                int numberOfProducts = Integer.parseInt(splitLine[1]);

                if(numberOfProducts == 0) {
                    line = reader.readLine();
                    continue;
                }

                ArrayList<Future<Boolean>> list = new ArrayList<>();
                for(int i = 1; i <= numberOfProducts; i++) {
                    list.add(tpe.submit(new Task(order, i)));
                }

                boolean status = true;

                for (Future<Boolean> element : list) {
                    if (!element.get()) {
                        status = false;
                    }
                }

                // Await answers and write to file.
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
        String readPath = Tema2.folderPath + "/order_products.txt";
        BufferedReader read = new BufferedReader(new FileReader(readPath));
        int count = 0;

        String line = read.readLine();
        while (line != null) {
            String[] splitLine = line.split(",");

            if (order.equals(splitLine[0])) count++;
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
        output.close();
        outputProducts.close();
    }
}
