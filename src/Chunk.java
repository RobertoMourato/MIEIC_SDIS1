import java.io.File;

public class Chunk {
    private String fileId;
    private int chunkNo;
    private int replicationDegree;
    private int currentReplicationId;
    private int size;

    /**
     * Constructor
     */
    public Chunk(String fileId, int chunkNo, int replicationDegree, int size) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.replicationDegree = replicationDegree;
        this.size = size;
    }

    /**
     * Getters
     */
    public String getFileId() {
        return this.fileId;
    }

    public int getChunkNo() {
        return this.chunkNo;
    }

    public int getReplicationDegree() {
        return this.replicationDegree;
    }

    public int getCurrentReplicationId() {
        return this.currentReplicationId;
    }

    public int getSize() {
        return this.size;
    }

    public String getIdentifier() {
        return  fileId + "_" + chunkNo;
    }

    public static File getFileChunk(int peerID, String fileId){
        String filePath = peerID + "/" + fileId;
        return new File(filePath);
    }

    /**
     * Setters
     */
}
