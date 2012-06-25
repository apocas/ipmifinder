package ipmifinder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author pedrodias petermdias@gmail.com
 * 
 */
public class Main {

    public static ConcurrentLinkedQueue<String> subnets = new ConcurrentLinkedQueue<String>();
    public static int max_threads = 500;
    public static AtomicInteger done = new AtomicInteger(0);
    public static int total = 0;

    public static void main(String... args) throws Exception {
        if (args.length < 2) {
            System.out.println("usage: java -jar IPMIFinder -r 192.168.20.1-192.168.20.254 OR java -jar IPMIFinder -f ranges.txt");
            System.exit(0);
        }

        if (args[0].contains("-f")) {
            readLines(args[1]);
        } else if (args[0].contains("-r")) {
            subnets.addAll(Arrays.asList(args[1]));
        }

        List<String> ll = Arrays.asList(subnets.toArray(new String[0]));
        Collections.shuffle(ll);
        subnets = new ConcurrentLinkedQueue<String>(ll);

        total = subnets.size();

        System.out.println("IPMIFinder v0.1");
        System.out.println(total + " subnets loaded");
        System.out.println("Scanning...");

        for (int i = 0; i < max_threads; i++) {
            new IPMIFinder().start();
        }

        System.out.println("Threads launched...");

        while (true) {
            Scanner s = new Scanner(System.in);
            try {
                s.nextLine();
                status();
            } catch (Exception e) {
            }
        }
    }

    public static void status() {
        int dones = done.get();
        System.out.println("");
        System.out.println("###########################");
        System.out.println("STATUS " + dones + "/" + total + " - " + (int) (((float) dones / (float) total) * 100.0f) + "%");
        System.out.println(Thread.activeCount() + " THREADS RUNNING");
        System.out.println("###########################");
        System.out.println("");
    }

    public static void loadSubnet(String subnet) {
        String[] aux = subnet.split("-");
        String[] auxb = aux[0].split("\\.");
        String[] auxe = aux[1].split("\\.");

        int ab = Integer.parseInt(auxb[0]);
        int bb = Integer.parseInt(auxb[1]);
        int cb = Integer.parseInt(auxb[2]);

        int ae = Integer.parseInt(auxe[0]);
        int be = Integer.parseInt(auxe[1]);
        int ce = Integer.parseInt(auxe[2]);

        int dd = ce - cb;
        int ddd = be - bb;


        if (dd == 0 && ddd == 0) {
            subnets.add(subnet);
        } else {
            for (int y = 0; y <= ddd; y++) {
                for (int i = 0; i <= dd; i++) {
                    String subnetb = ab + "." + (int) (bb + y) + "." + (int) (cb + i) + ".1";
                    String subnete = ab + "." + (int) (bb + y) + "." + (int) (cb + i) + ".254";
                    subnets.add(subnetb + "-" + subnete);
                }
            }
        }
    }

    public static void readLines(String filename) throws IOException {
        FileReader fileReader = new FileReader(filename);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            loadSubnet(line);
        }
        bufferedReader.close();
    }
}
