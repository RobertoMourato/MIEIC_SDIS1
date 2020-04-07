import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer implements RMI {
    private int peerId;
    private Storage storage;
    private ControlChannel controlChannel;
    private BackupChannel backupChannel;
    private ExecutorService executor;

    /**
     * Constructor
     */
    Peer(int peerId) {
        this.peerId = peerId;

        executor = Executors.newScheduledThreadPool(150);

        try {
            this.controlChannel = new ControlChannel( this, "224.0.1.0", 9998);
            this.backupChannel = new BackupChannel( this, "224.0.0.1", 9999);

            executor.execute(this.controlChannel);
            executor.execute(this.backupChannel);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Couldn't create channels for peer " + peerId );
        }

    }

    public int getPeerId() {
        return peerId;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Main
     */
    public static void main(String[] args) throws RemoteException, AlreadyBoundException {

        for (int i = 2; i <= 4; i++){
            new Peer(i);
        }

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
//        storage.addFileData(fileData);

        for (int i = 0; i < fileData.getChunks().size(); i++) {
            Chunk chunk = fileData.getChunks().get(i);
            String header = "1.0 PUTCHUNK " + this.peerId + " " + fileData.getFileId() + " " + chunk.getChunkNo() + " " + replicationDegree + "\r\n\r\n";
            //System.out.println(header);
            byte[] encodedHeader = header.getBytes(StandardCharsets.US_ASCII);
            byte[] body = chunk.getContent();
            byte[] message = new byte[encodedHeader.length + body.length];
            // concatenate encodedHeader with body
            System.arraycopy(encodedHeader, 0, message, 0, encodedHeader.length);
            System.arraycopy(body, 0, message, encodedHeader.length, body.length);
            /**FALTA PARTE COM OS THREADS QUE AINDA N PERCEBI MT BEM, TBM TENHO DE VER MELHOR A PARTE DE MULTICAST E OS CHANNELS*/


            this.backupChannel.sendMessage(message);
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
        byte[] mes = "ola".getBytes();
        try {
            this.controlChannel.sendMessage(mes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "state";
    }
}