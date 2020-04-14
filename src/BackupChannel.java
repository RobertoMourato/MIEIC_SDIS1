import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

public class BackupChannel extends Channel {

    BackupChannel(Peer peer, String inet_address, int port) throws IOException {
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

                handleMessage(bufferCopy);

//                System.out.println("MDB " + peer.getPeerId() + " data: " + packet.getLength());
            }

        } catch (Exception e){
            System.out.println("Bad exception");
            e.printStackTrace();
        }

    }
}
