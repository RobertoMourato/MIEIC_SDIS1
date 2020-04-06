import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class FileData {
    private static int MAX_SIZE_CHUNK = 64000;
    private String fileId;
    private int replicationDegree;
    private File file;
    private ArrayList<Chunk> chunks;

    /**
     * Constructor
     */
    public FileData(String filePath, int replicationDegree) throws IOException, NoSuchAlgorithmException {
        this.file = new File(filePath);
        this.replicationDegree = replicationDegree;
        this.fileId = FileData.generateFileId(this.file);
        this.chunks = generateChunksFromFile(this.file, this.fileId);
    }

    /**
     * Getters
     */
    public String getFileId() {
        return this.fileId;
    }

    public int getReplicationDegree() {
        return this.replicationDegree;
    }

    public java.io.File getFile() {
        return this.file;
    }

    public ArrayList<Chunk> getChunks() {
        return this.chunks;
    }

    /**
     * Other Methods
     */
    public static String generateFileId(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return bytesToHex(md.digest(Files.readAllBytes(file.toPath())));

    }

    private ArrayList<Chunk> generateChunksFromFile(File file, String fileId) {
        ArrayList<Chunk> chunks = new ArrayList<>();
        int chunkNumber = 0;
        int bytesRead = 0;
        byte[] curChunk = new byte[MAX_SIZE_CHUNK];

        try (FileInputStream fis = new FileInputStream(file)) {
            while ((bytesRead = fis.read(curChunk)) > 0) {
                chunks.add(new Chunk(fileId, chunkNumber++, Arrays.copyOf(curChunk, bytesRead), this.replicationDegree));
            }
            if (file.length() % MAX_SIZE_CHUNK == 0) {
                chunks.add(new Chunk(fileId, chunkNumber, null, this.replicationDegree));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return chunks;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
