import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

public class BackupChannel extends Channel {

    BackupChannel(int peer, String inet_address, int port) throws IOException {
        super(peer, inet_address, port);
    }

    @Override
    public void run() {

        try {

            this.multicastSocket.joinGroup(this.multicastInetAddress);

            while (true) {

                byte[] buf = new byte[65024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                this.multicastSocket.receive(packet);

                byte[] bufferCopy = Arrays.copyOf(buf, packet.getLength());

                System.out.println("MDB " + peer + " data: " + packet.getLength());

                File tmp = new File(peer + "/" + packet.getLength());
                tmp.getParentFile().mkdirs();
                tmp.createNewFile();

            }

        } catch (Exception e){
            System.out.println("Bad exception");
            e.printStackTrace();
        }

    }
}
