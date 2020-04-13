import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

public class RestoreChannel extends Channel {

    RestoreChannel(Peer peer, String inet_address, int port) throws IOException {
        super(peer, inet_address, port);
    }

    @Override
    public void run() {

        try {

            this.multicastSocket.joinGroup(this.multicastInetAddress);

            while (true) {
                byte[] buf;
                if(this.peer.getVersion().equals("1.0")){
                    buf = new byte[65024];
                }else{
                    buf = new byte[1024];
                }

                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                this.multicastSocket.receive(packet);

                byte[] bufferCopy = Arrays.copyOf(buf, packet.getLength());

                handleMessage(bufferCopy);

                System.out.println("MDR " + peer.getPeerId() + " data: " + packet.getLength());
            }

        } catch (Exception e){
            System.out.println("Bad exception");
            e.printStackTrace();
        }

    }
}
