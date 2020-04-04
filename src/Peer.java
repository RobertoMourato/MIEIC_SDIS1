import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Peer implements RMI{

    public static void main(String[] args) throws RemoteException, AlreadyBoundException {

        Peer peer = new Peer();
        RMI sender = (RMI) UnicastRemoteObject.exportObject(peer, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.bind("ououou", sender);
    }

    @Override
    public String cenas() {
        return "qualquercoisa";
    }
}