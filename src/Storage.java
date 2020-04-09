import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Storage {
    private ArrayList<FileData> filesData;
    private ArrayList<Chunk> storedChunks;
    private ConcurrentHashMap<String, Integer> chunkOccurrences;

    /**
     * Constructor
     */
    Storage() {
        filesData = new ArrayList<>();
        storedChunks = new ArrayList<>();
        chunkOccurrences = new ConcurrentHashMap<>();
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

    /**
     * Other Methods
     */
    public void addFileData(FileData fileData) {
        filesData.add(fileData);
    }
}
