import java.util.ArrayList;

public class Storage {
    ArrayList<FileData> filesData;
    ArrayList<Chunk> storedChunks;

    /**
     * Constructor
     */
    Storage() {
        filesData = new ArrayList<>();
        storedChunks = new ArrayList<>();
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

    /**
     * Other Methods
     */
    public void addFileData(FileData fileData) {
        filesData.add(fileData);
    }
}
