import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;

public class Peer implements RMI{

    int id;
    /**Main*/
    public static void main(String[] args) throws RemoteException, AlreadyBoundException {

        Peer peer = new Peer(1);
        RMI sender = (RMI) UnicastRemoteObject.exportObject(peer, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.rebind("ououou", sender);
    }

    Peer (int id){
        this.id = id;
    }

    @Override
    public String backup(String filePath, int replicationDegree) throws IOException, NoSuchAlgorithmException {

        FileData fileData = new FileData(filePath, replicationDegree);

        return "backup " + fileData.getFileId();
    }

    @Override
    public String restore(String filePath) throws RemoteException {
        return "restore";
    }

    @Override
    public String delete(String filePath) throws RemoteException {
        return "delete";
    }

    @Override
    public String reclaim(int diskSpace) throws RemoteException {
        return "reclaim";
    }

    @Override
    public String state() throws RemoteException {
        return "state";
    }
}