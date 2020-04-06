import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;

public interface RMI extends Remote {
    /**
     * Methods
     */
    String backup(String filePath, int replicationDegree) throws RemoteException, IOException, NoSuchAlgorithmException;

    String restore(String filePath) throws RemoteException;

    String delete(String filePath) throws RemoteException;

    String reclaim(int diskSpace) throws RemoteException;

    String state() throws RemoteException;
}
