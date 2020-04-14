import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Storage {
    private ConcurrentHashMap<String, FileData> filesData;
    private ConcurrentHashMap<String, Chunk> storedChunks;
    private ConcurrentHashMap<String, Integer> storedChunksOccurrences;
    private ConcurrentHashMap<String, Boolean> otherPeersWantedChunks;
    private ConcurrentHashMap<String, Boolean> selfPeerWantedChunks;
    private ConcurrentHashMap<String, Boolean> storedSelfWantedChunks;
    private ConcurrentHashMap<String, Boolean> handleLowOccurences;
    private int occupiedSpace;
    private int maxOccupiedSpace;   // in bytes

    /**
     * Constructor
     */
    Storage() {
        filesData = new ConcurrentHashMap<>();
        storedChunks = new ConcurrentHashMap<>();
        storedChunksOccurrences = new ConcurrentHashMap<>();
        otherPeersWantedChunks = new ConcurrentHashMap<>();
        selfPeerWantedChunks = new ConcurrentHashMap<>();
        storedSelfWantedChunks = new ConcurrentHashMap<>();
        handleLowOccurences = new ConcurrentHashMap<>();
        occupiedSpace = 0;
        maxOccupiedSpace = 1000000000;
    }

    /**
     * Getters
     */
    public ConcurrentHashMap<String, FileData> getFilesData() {
        return filesData;
    }

    public ConcurrentHashMap<String, Integer> getStoredChunksOccurrences() {
        return storedChunksOccurrences;
    }

    public ConcurrentHashMap<String, Boolean> getOtherPeersWantedChunks() {
        return otherPeersWantedChunks;
    }

    public ConcurrentHashMap<String, Boolean> getSelfPeerWantedChunks() {
        return selfPeerWantedChunks;
    }

    public ConcurrentHashMap<String, Boolean> getStoredSelfWantedChunks() {
        return storedSelfWantedChunks;
    }

    public ConcurrentHashMap<String, Chunk> getStoredChunks() {
        return storedChunks;
    }

    public ConcurrentHashMap<String, Boolean> getHandleLowOccurences() {
        return handleLowOccurences;
    }

    public int getOccupiedSpace() {
        return occupiedSpace;
    }

    public int getMaxOccupiedSpace() {
        return maxOccupiedSpace;
    }

    /**
     * Other Methods
     */

    public void setMaxOccupiedSpace(int maxOccupiedSpace) {
        this.maxOccupiedSpace = maxOccupiedSpace;
    }

    public void addFileData(FileData fileData) {
        filesData.put(fileData.getFileId(), fileData);
    }

    public synchronized Chunk getStoredChunk(String chunkID){
        return storedChunks.get(chunkID);
    }

    public synchronized boolean deleteStoredChunk(String chunkID){
        Chunk removedChunk = storedChunks.remove(chunkID);
        if (removedChunk != null){
            occupiedSpace -= removedChunk.getSize();
            return true;
        }
        return false;
    }

    public synchronized boolean addStoredChunk(Chunk chunk){
        Chunk previousChunk = storedChunks.get(chunk.getIdentifier());
        int deltaSpace = 0;
        if (previousChunk != null){
            deltaSpace -= previousChunk.getSize();
        }
        deltaSpace += chunk.getSize();

        if (deltaSpace + occupiedSpace > maxOccupiedSpace)
            return false;

        storedChunks.put(chunk.getIdentifier(), chunk);
        occupiedSpace += deltaSpace;

        return true;
    }

}
