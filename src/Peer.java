import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;

public class Peer implements RMI {
    private int peerId;
    private Storage storage;

    /**
     * Constructor
     */
    Peer(int peerId) {
        this.peerId = peerId;
    }

    /**
     * Main
     */
    public static void main(String[] args) throws RemoteException, AlreadyBoundException {
        Peer peer = new Peer(1);
        RMI sender = (RMI) UnicastRemoteObject.exportObject(peer, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.rebind("ououou", sender);
    }

    /**
     * Other Methods
     */
    @Override
    public String backup(String filePath, int replicationDegree) throws IOException, NoSuchAlgorithmException {
        FileData fileData = new FileData(filePath, replicationDegree);
        storage.addFileData(fileData);

        for (int i = 0; i < fileData.getChunks().size(); i++) {
            Chunk chunk = fileData.getChunks().get(i);
            String header = "1.0 PUTCHUNK" + this.peerId + " " + fileData.getFileId() + " " + chunk.getChunkNo() + replicationDegree + "\r\n\r\n";
            //System.out.println(header);
            byte[] encodedHeader = header.getBytes("US-ASCII");
            byte[] body = chunk.getContent();
            byte[] message = new byte[encodedHeader.length + body.length];
            // concatenate encodedHeader with body
            System.arraycopy(encodedHeader, 0, message, 0, encodedHeader.length);
            System.arraycopy(body, 0, message, encodedHeader.length, body.length);
            /**FALTA PARTE COM OS THREADS QUE AINDA N PERCEBI MT BEM, TBM TENHO DE VER MELHOR A PARTE DE MULTICAST E OS CHANNELS*/
        }

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