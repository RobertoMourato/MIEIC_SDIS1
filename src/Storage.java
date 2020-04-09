import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Storage {
    private ArrayList<FileData> filesData;
    private ArrayList<Chunk> storedChunks;
    private ConcurrentHashMap<String, Integer> chunkOccurrences;
    private ConcurrentHashMap<String, Boolean> wantedChunks;

    /**
     * Constructor
     */
    Storage() {
        filesData = new ArrayList<>();
        storedChunks = new ArrayList<>();
        chunkOccurrences = new ConcurrentHashMap<>();
        wantedChunks = new ConcurrentHashMap<>();
    }

    /**
     * Getters
     */
    public ArrayList<FileData> getFilesData() {
        return filesData;
    }

    public ArrayList<Chunk> getStoredChunks() {
        return storedChunks;
    }

    public ConcurrentHashMap<String, Integer> getChunkOccurrences() {
        return chunkOccurrences;
    }

    public ConcurrentHashMap<String, Boolean> getWantedChunks() {
        return wantedChunks;
    }

    /**
     * Other Methods
     */
    public void addFileData(FileData fileData) {
        filesData.add(fileData);
    }
}
