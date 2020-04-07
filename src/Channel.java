import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

abstract class Channel implements Runnable {

    Peer peer;
    MulticastSocket multicastSocket;
    int multicastPort;
    InetAddress multicastInetAddress;

    Channel(Peer peer, String inet_address, int port) throws IOException {
        this.peer = peer;
        this.multicastPort = port;
        this.multicastSocket = new MulticastSocket(this.multicastPort);
        this.multicastInetAddress = InetAddress.getByName(inet_address);
    }

    void sendMessage(byte[] message) throws IOException {

        MulticastSocket senderSocket = new MulticastSocket();

        DatagramPacket msgPacket = new DatagramPacket(message, message.length, multicastInetAddress, multicastPort);
        senderSocket.send(msgPacket);

    }

    void handleMessage(byte[] message){
        this.peer.getExecutor().execute(new MessageHandler(this.peer, message));
    }

}