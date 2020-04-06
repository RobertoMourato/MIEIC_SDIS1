import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

abstract class Channel implements Runnable {

    int peer;
    MulticastSocket multicastSocket;
    int multicastPort;
    InetAddress multicastInetAddress;

    Channel(int peer, String inet_address, int port) throws IOException {
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

}