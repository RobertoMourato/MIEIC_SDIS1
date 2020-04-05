import java.util.ArrayList;

public class Storage {

    ArrayList<FileData> filesData;
    ArrayList<Chunk> storedChunks;

    public ArrayList<FileData> getFilesData() {
        return filesData;
    }

    public ArrayList<Chunk> getStoredChunks() {
        return storedChunks;
    }

    Storage(){
        filesData = new ArrayList<>();
        storedChunks = new ArrayList<>();
    }
}
