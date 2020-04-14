import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
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
    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private InputStream inputStream = null;
    private String version;

    /**
     * Constructor
     */
    Peer(int peerId, int version, String MCAddress, int MCPort, String MDBAddress, int MDBPort, String MDRAddress, int MDRPort) {
        this.peerId = peerId;
        if (version == 1) {
            this.version = "1.0";
        } else {
            this.version = "2.0";
        }
        this.storage = new Storage();

        tryToGetPreviousStorageState();

        Runtime.getRuntime().addShutdownHook(new Thread(this::saveStorageState));

        executor = Executors.newScheduledThreadPool(150);

        try {
            this.controlChannel = new ControlChannel(this, MCAddress, MCPort);
            this.backupChannel = new BackupChannel(this, MDBAddress, MDBPort);
            this.restoreChannel = new RestoreChannel(this, MDRAddress, MDRPort);

            executor.execute(this.controlChannel);
            executor.execute(this.backupChannel);
            executor.execute(this.restoreChannel);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Couldn't create channels for peer " + peerId);
        }


        try {

            RMI sender = (RMI) UnicastRemoteObject.exportObject(this, 0);

            Registry registry = LocateRegistry.getRegistry();

            registry.rebind("peer-" + peerId, sender);

        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }

    public ControlChannel getControlChannel() {
        return controlChannel;
    }

    public RestoreChannel getRestoreChannel() {
        return restoreChannel;
    }

    public BackupChannel getBackupChannel() {
        return backupChannel;
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

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public Socket getSocket() {
        return socket;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Main
     */
    public static void main(String[] args) {
        int version;

        if (args.length != 8){
            System.out.println("Format invalid, please try again.");
            System.out.println("Format: Peer <version> <n_peers> <MC_address> <MC_port> <MDB_address> <MDB_port> <MDR_address> <MDR_port>");
        }

        version = Integer.parseInt(args[0]);
        if (version != 1 && version != 2) {
            System.out.println("ERROR: Peer arguments invalid: Version(1 or 2)");
            return;
        }

        int nPeers = Integer.parseInt(args[1]);
        String MCAddress = args[2];
        int MCPort = Integer.parseInt(args[3]);
        String MDBAddress = args[4];
        int MDBPort = Integer.parseInt(args[5]);
        String MDRAddress = args[6];
        int MDRPort = Integer.parseInt(args[7]);

        for (int i = 1; i <= nPeers; i++) {
            new Peer(i, version, MCAddress, MCPort, MDBAddress, MDBPort, MDRAddress, MDRPort);
        }

    }

    private void saveStorageState() {

        String filename = this.getPeerId() + "/serialization.ser";

        File file = new File(filename);

        try {

            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            FileOutputStream fis = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fis);

            out.writeObject(this.getStorage());

            out.close();
            fis.close();

            System.out.println("Saved state of peer " + this.getPeerId());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void tryToGetPreviousStorageState() {

        String filename = this.getPeerId() + "/serialization.ser";

        File file = new File(filename);

        if (!file.exists())
            return;

        try {

            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fis);

            this.storage = (Storage)in.readObject();

            in.close();
            fis.close();
            System.out.println("Loaded previous state of peer " + this.getPeerId());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

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
                    TimeUnit.MILLISECONDS.sleep(1000 * (1 << tries));
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
    public String restore(String filePath) {
        try {
            this.serverSocket = new ServerSocket(10010);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        if (this.storage.getFilesData().get(fileID) != null) {
            System.out.println("ON RESTORE: File != null");
            backedUp = true;
            FileData fileData = this.storage.getFilesData().get(fileID);

            for (int i = 0; i < fileData.getChunks().size(); i++) {
                String header = this.version + " GETCHUNK " + this.peerId + " " + fileData.getFileId() + " " + fileData.getChunks().get(i).getChunkNo() + "\r\n\r\n";
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

                if (this.version.equals("2.0")) {
                    try {
                        String filePathAux = this.peerId + "/wanted/" + fileData.getChunks().get(i).getIdentifier();
                        File tmp = new File(filePathAux);
                        tmp.getParentFile().mkdirs();

                        this.socket = this.serverSocket.accept();
                        this.socket.setReuseAddress(true);
                        this.inputStream = this.socket.getInputStream();

                        byte[] buf = new byte[64000];
                        int count = this.inputStream.read(buf);

                        tmp.createNewFile();
                        FileOutputStream writeToFile = new FileOutputStream(tmp);
                        writeToFile.write(buf, 0, count);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (this.version.equals("2.0")) {
            try {
                this.socket.close();
                this.serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        boolean allAvailable = true;

        if (backedUp) {
            FileData fileData = this.storage.getFilesData().get(fileID);

            for (int i = 0; i < fileData.getChunks().size(); i++) {
                Chunk curChunk = fileData.getChunks().get(i);
                System.out.println("ON RESTORE: Chunk " + curChunk.getIdentifier());
                if (this.storage.getStoredSelfWantedChunks().get(curChunk.getIdentifier()) == null ||
                        !this.storage.getStoredSelfWantedChunks().get(curChunk.getIdentifier())) {
                    allAvailable = false;
                    break;
                }
            }

            if (allAvailable) {

                File endFile = new File("restored/" + this.peerId + "/" + filePath);
                endFile.getParentFile().mkdirs();
                FileOutputStream writeToFile = null;

                try {
                    endFile.createNewFile();
                    writeToFile = new FileOutputStream(endFile);
                    System.out.println("ON RESTORE: Create file and writer");
                    //writeToFile.write(arguments.get(5).getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }


                for (int i = 0; i < fileData.getChunks().size(); i++) {
                    Chunk curChunk = fileData.getChunks().get(i);
                    String fileChunkPath = this.getPeerId() + "/wanted/" + curChunk.getIdentifier();
                    File tmp = new File(fileChunkPath);

                    try {
                        writeToFile.write(Files.readAllBytes(tmp.toPath()));
                        System.out.println("ON RESTORE: Write " + curChunk.getIdentifier());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }

                return "Restored file " + endFile;
            }
        }
        return "Failed restoring file " + filePath;
    }

    @Override
    public String delete(String filePath) throws RemoteException {

        String fileID = "NULL";
        try {
            fileID = FileData.generateFileId(new File(filePath));
            System.out.println("ON DELETE: get file ID");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (this.storage.getFilesData().get(fileID) != null) {

            FileData fileData = this.storage.getFilesData().get(fileID);


            String header = "1.0 DELETE " + this.peerId + " " + fileData.getFileId() + "\r\n\r\n";

            try {
                this.controlChannel.sendMessage(header.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }


            return "DELETE " + filePath + " SUCCESSFUL";
        }


        return "DELETE " + filePath + " FAILED";
    }

    @Override
    public String reclaim(int diskSpace) throws RemoteException {

        int maxSpace = diskSpace * 1000;
        this.getStorage().setMaxOccupiedSpace(maxSpace);


        if (this.getStorage().getOccupiedSpace() > maxSpace){

            Iterator<ConcurrentHashMap.Entry<String, Chunk>> it = this.getStorage().getStoredChunks().entrySet().iterator();
            while (it.hasNext() && this.getStorage().getOccupiedSpace() > maxSpace) { // Delete chunks with higher priority than needed
                ConcurrentHashMap.Entry<String, Chunk> pair = it.next();

                Chunk chunk = pair.getValue();
                String chunkID = pair.getKey();

                if (this.getStorage().getStoredChunksOccurrences().get(chunkID)
                    > chunk.getReplicationDegree()){

                    File file = new File(this.getPeerId() + "/" + chunkID);
                    file.delete();

                    this.getStorage().deleteStoredChunk(chunkID);
                    this.getStorage().getStoredChunksOccurrences().put(chunkID,
                            this.getStorage().getStoredChunksOccurrences().get(chunkID) - 1);

                    String header = this.version + " REMOVED " + this.peerId + " " + chunk.getFileId() + " " + chunk.getChunkNo() + "\r\n\r\n";

                    try {
                        TimeUnit.MILLISECONDS.sleep((long) (Math.random() * 400));
                        this.controlChannel.sendMessage(header.getBytes());
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println("REMOVED " + pair.getValue().getIdentifier());

                    it.remove(); // deletes pair, avoids a ConcurrentModificationException
                }

            }


            it = this.getStorage().getStoredChunks().entrySet().iterator();
            while (it.hasNext() && this.getStorage().getOccupiedSpace() > maxSpace) { // Delete chunks not caring for priority
                ConcurrentHashMap.Entry<String, Chunk> pair = it.next();

                Chunk chunk = pair.getValue();
                String chunkID = pair.getKey();


                this.getStorage().deleteStoredChunk(chunkID);
                this.getStorage().getStoredChunksOccurrences().put(chunkID,
                        this.getStorage().getStoredChunksOccurrences().get(chunkID) - 1);

                String header = this.version + " REMOVED " + this.peerId + " " + chunk.getFileId() + " " + chunk.getChunkNo() + "\r\n\r\n";

                try {
                    TimeUnit.MILLISECONDS.sleep((long) (Math.random() * 400));
                    this.controlChannel.sendMessage(header.getBytes());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                File file = new File(this.getPeerId() + "/" + chunkID);


                if (this.getStorage().getStoredChunksOccurrences().get(chunkID) == 0){  // this peer was the only with this chunk
                    this.getStorage().getHandleLowOccurences().put(chunkID, true);

                    byte[] body = new byte[0];

                    try {
                        body = Files.readAllBytes(file.toPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    header = this.getVersion() + " PUTCHUNK " + this.getPeerId() + " " + chunk.getFileId() + " " + chunk.getChunkNo() + " " + chunk.getReplicationDegree() + "\r\n\r\n";
                    byte[] encodedHeader = header.getBytes(StandardCharsets.US_ASCII);
                    byte[] message = new byte[encodedHeader.length + body.length];
                    System.arraycopy(encodedHeader, 0, message, 0, encodedHeader.length);
                    System.arraycopy(body, 0, message, encodedHeader.length, body.length);

                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                        if (this.getStorage().getHandleLowOccurences().get(chunkID)) {
                            this.getBackupChannel().sendMessage(message);
                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }

                }

                file.delete();

                System.out.println("REMOVED " + pair.getValue().getIdentifier());

                it.remove(); // deletes pair, avoids a ConcurrentModificationException

            }

        }

        return "reclaim";
    }

    @Override
    public String state() throws RemoteException {
        StringBuilder status = new StringBuilder(14);

        status.append("Files initiated by this peer:\n");

        for (ConcurrentHashMap.Entry<String, FileData> pair : this.getStorage().getFilesData().entrySet()){
            status.append(pair.getValue().getFilePath()).append(" ").append(pair.getValue().getFileId())
                    .append(" ").append(pair.getValue().getReplicationDegree()).append("\n");
            for (int i = 0; i < pair.getValue().getChunks().size(); i++){
                Chunk chunk = pair.getValue().getChunks().get(i);
                status.append("   ").append(chunk.getIdentifier()).append(" ")
                        .append(this.getStorage().getStoredChunksOccurrences().get(chunk.getIdentifier()))
                        .append("\n");
            }
        }

        status.append("Chunks stored in this peer:\n");
        for (ConcurrentHashMap.Entry<String, Chunk> pair : this.getStorage().getStoredChunks().entrySet()){
            status.append(pair.getKey()).append(" ").append(pair.getValue().getSize()).append(" ")
                    .append(this.getStorage().getStoredChunksOccurrences().get(pair.getKey())).append("\n");
        }

        status.append("Max occupied Space: ").append(this.getStorage().getMaxOccupiedSpace()).append("\n");
        status.append("Occupied Space: ").append(this.getStorage().getOccupiedSpace()).append("\n");


        return status.toString();
    }
}