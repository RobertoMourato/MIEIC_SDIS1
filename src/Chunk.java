
public class Chunk {
    private String fileId;
    private int chunkNo;
    private int replicationDegree;
    private int currentReplicationId;
    private byte[] content;
    private int size;

    /**
     * Constructor
     */
    public Chunk(String fileId, int chunkNo, byte[] content, int replicationDegree) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.content = content;
        this.replicationDegree = replicationDegree;
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

    public byte[] getContent() {
        return this.content;
    }

    public int getSize() {
        return this.size;
    }

    /**
     * Setters
     */
    public void setContent(byte[] content) {
        this.content = content;
    }
}
