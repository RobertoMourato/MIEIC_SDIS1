import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Storage {
    private ConcurrentHashMap<String, FileData> filesData;
    private ConcurrentHashMap<String, Chunk> storedChunks;
    private ConcurrentHashMap<String, Integer> storedChunksOccurrences;
    private ConcurrentHashMap<String, Boolean> otherPeersWantedChunks;
    private ConcurrentHashMap<String, Boolean> selfPeerWantedChunks;
    private ConcurrentHashMap<String, Boolean> storedSelfWantedChunks;

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
    }

    /**
     * Getters
     */
    public ConcurrentHashMap<String, FileData> getFilesData() {
        return filesData;
    }

    public ConcurrentHashMap<String, Chunk> getStoredChunks() {
        return storedChunks;
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

    /**
     * Other Methods
     */
    public void addFileData(FileData fileData) {
        filesData.put(fileData.getFileId(), fileData);
    }

}
