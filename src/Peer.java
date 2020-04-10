import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Peer implements RMI {
    private int peerId;
    private Storage storage;
    private ControlChannel controlChannel;
    private BackupChannel backupChannel;
    private RestoreChannel restoreChannel;
    private ExecutorService executor;

    /**
     * Constructor
     */
    Peer(int peerId) {
        this.peerId = peerId;
        this.storage = new Storage();
        executor = Executors.newScheduledThreadPool(150);

        try {
            this.controlChannel = new ControlChannel(this, "224.0.1.0", 9998);
            this.backupChannel = new BackupChannel(this, "224.0.0.1", 9999);
            this.restoreChannel = new RestoreChannel(this, "224.0.0.2", 9997);

            executor.execute(this.controlChannel);
            executor.execute(this.backupChannel);
            executor.execute(this.restoreChannel);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Couldn't create channels for peer " + peerId);
        }

    }

    public ControlChannel getControlChannel() {
        return controlChannel;
    }

    public RestoreChannel getRestoreChannel() {
        return restoreChannel;
    }

    public Storage getStorage() {
        return storage;
    }

    public int getPeerId() {
        return this.peerId;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Main
     */
    public static void main(String[] args) throws RemoteException, AlreadyBoundException {

        for (int i = 2; i <= 4; i++) {
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
        byte[] content = Files.readAllBytes(Paths.get(filePath));
        storage.addFileData(fileData);

        for (int i = 0; i < fileData.getChunks().size(); i++) {
            this.storage.getStoredChunksOccurrences().put(fileData.getFileId() + "_" + i, 0);
        }

        int currentSize = 0;

        for (int i = 0; i < fileData.getChunks().size(); i++) {
            Chunk chunk = fileData.getChunks().get(i);

            int tries = 0;
            boolean done;

            do {
                done = true;

                if (this.storage.getStoredChunksOccurrences().get(fileData.getFileId() + "_" + i) >= replicationDegree) {
                    currentSize += chunk.getSize();
                    continue;
                }

                done = false;


                String header = "1.0 PUTCHUNK " + this.peerId + " " + fileData.getFileId() + " " + chunk.getChunkNo() + " " + replicationDegree + "\r\n\r\n";
                System.out.println(header);
                byte[] encodedHeader = header.getBytes(StandardCharsets.US_ASCII);

                byte[] body = new byte[chunk.getSize()];
                System.arraycopy(content, currentSize, body, 0, chunk.getSize());

                byte[] message = new byte[encodedHeader.length + body.length];
                System.arraycopy(encodedHeader, 0, message, 0, encodedHeader.length);
                System.arraycopy(body, 0, message, encodedHeader.length, body.length);

                this.storage.getStoredChunksOccurrences().put(fileData.getFileId() + "_" + i, 0);

                this.backupChannel.sendMessage(message);

                try {
                    TimeUnit.MILLISECONDS.sleep(100*(1<<tries));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } while (tries++ < 5 && !done);

            if (!done)
                return "backup " + fileData.getFileId() + " FAILED";

//                currentSize += chunk.getSize();
        }

//            try {
//                TimeUnit.MILLISECONDS.sleep(1000*(1<<tries));
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }



        return "backup " + fileData.getFileId() + " SUCCESSFUL";
    }

    @Override
    public String restore(String filePath) throws RemoteException {
        boolean backedUp = false;
        String fileID = "NULL";
        try {
            fileID = FileData.generateFileId(new File(filePath));
            System.out.println("ON RESTORE: get file ID");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (this.storage.getFilesData().get(fileID) != null){
            System.out.println("ON RESTORE: File != null");
            backedUp = true;
            FileData fileData = this.storage.getFilesData().get(fileID);

            for (int i = 0; i < fileData.getChunks().size(); i++){
                String header = "1.0 GETCHUNK " + this.peerId + " " + fileData.getFileId() + " " + fileData.getChunks().get(i).getChunkNo() + "\r\n\r\n";
                //System.out.println(header);
                byte[] message = header.getBytes(StandardCharsets.US_ASCII);

                this.getStorage().getSelfPeerWantedChunks().put(fileData.getChunks().get(i).getIdentifier(), true);

                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    this.controlChannel.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean allAvailable = true;

        if (backedUp){
            FileData fileData = this.storage.getFilesData().get(fileID);

            for (int i = 0; i < fileData.getChunks().size(); i++){
                Chunk curChunk = fileData.getChunks().get(i);
                System.out.println("ON RESTORE: Chunk " + curChunk.getIdentifier() );
                if (this.storage.getStoredSelfWantedChunks().get(curChunk.getIdentifier()) == null ||
                        !this.storage.getStoredSelfWantedChunks().get(curChunk.getIdentifier())){
                    allAvailable = false;
                    break;
                }
            }

            if (allAvailable){

                File endFile = new File("restored/" + this.peerId + "/" + filePath);
                endFile.getParentFile().mkdirs();
                FileOutputStream writeToFile = null;

                try {
                    endFile.createNewFile();
                    writeToFile = new FileOutputStream(endFile);
                    System.out.println("ON RESTORE: Create file and writer");
//                    writeToFile.write(arguments.get(5).getBytes());
                } catch (Exception e){
                    e.printStackTrace();
                }



                for (int i = 0; i < fileData.getChunks().size(); i++){
                    Chunk curChunk = fileData.getChunks().get(i);
                    String fileChunkPath = this.getPeerId() + "/wanted/" + curChunk.getIdentifier();
                    File tmp = new File(fileChunkPath);

                    try {
                        writeToFile.write(Files.readAllBytes(tmp.toPath()));
                        System.out.println("ON RESTORE: Write " + curChunk.getIdentifier() );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }

                return "Restored file " + endFile;


//                String filePath = peer.getPeerId() + "/wanted/" + fileName;
//                File tmp = new File(filePath);
//                tmp.getParentFile().mkdirs();
//
//                this.peer.getStorage().getSelfPeerWantedChunks().put(fileName, false);
//                this.peer.getStorage().getStoredSelfWantedChunks().put(fileName, true);
//
//                try {
//                    tmp.createNewFile();
//                    FileOutputStream writeToFile = new FileOutputStream(tmp);
//                    writeToFile.write(arguments.get(5).getBytes());



            }
        }

//        for (int i = 0; i < this.storage.getFilesData().size(); i++) {
//            if (this.storage.getFilesData().get(i).getFile().getPath().equals(filePath)) {
//                backedUp = true;
//
//                for (int j = 0; j < this.storage.getFilesData().get(i).getChunks().size(); j++) {
//                    String header = "1.0 GETCHUNK " + this.peerId + " " + this.storage.getFilesData().get(i).getFileId() + " " + this.storage.getFilesData().get(i).getChunks().get(j).getChunkNo() + "\r\n\r\n";
//                    //System.out.println(header);
//                    byte[] message = header.getBytes(StandardCharsets.US_ASCII);
//                    try {
//                        this.controlChannel.sendMessage(message);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }

//        if (backedUp) {
//            return "Restored file " + filePath;
//        } else {
            return "Failed restoring file " + filePath;
//        }
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