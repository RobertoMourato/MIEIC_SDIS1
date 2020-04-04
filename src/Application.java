import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Application {
    public static void main(String[] args) {

        if (args.length < 3 || args.length > 4) {
            System.out.println("ERROR: App arguments invalid: Application <host>/<peer_id> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        try {


            String[] peerInfo = args[0].split("/");
            String host = peerInfo[0];
            String peerID = peerInfo[1];

            Registry registry = LocateRegistry.getRegistry(host);
            RMI peer = (RMI) registry.lookup("ououou");

            System.out.println(peer.cenas());

        } catch (Exception e) {
            System.out.println("Appplication exception: " + e.toString());
            e.printStackTrace();
        }

    }
}
