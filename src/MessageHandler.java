import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageHandler implements Runnable {
    Peer peer;
    byte[] message;

    MessageHandler(Peer peer, byte[] message){
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void run() {
        String mes = new String(this.message, StandardCharsets.US_ASCII);
        mes = mes.trim();
        String[] parameters = mes.split(" ");
        switch (parameters[1]){
            case "PUTCHUNK":
                System.out.println("PUTCHUNK");
                handlePutChunk();
                break;
            case "STORED":
                System.out.println("STORED");
                break;
            case "GETCHUNK":
                System.out.println("GETCHUNK");
                break;
            case "CHUNK":
                System.out.println("CHUNK");
                break;
            case "DELETE":
                System.out.println("DELETE");
                break;
            case "REMOVED":
                System.out.println("REMOVED");
                break;
            default:
                System.out.println("ERROR");
                System.out.println(parameters[1]);
                break;
        }
    }

    void handlePutChunk() {
        List<String> arguments = parseMessage(true, true, true);

        String fileName = peer.getPeerId() + "/" + arguments.get(3) + "_" + arguments.get(4);
        File tmp = new File(fileName);
        tmp.getParentFile().mkdirs();

        try {
            tmp.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] body = arguments.get(6).getBytes(StandardCharsets.US_ASCII);

        try {
            FileOutputStream writeToFile = new FileOutputStream(fileName);
            writeToFile.write(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    List<String> parseMessage(boolean hasChunkNumber, boolean hasReplicationDegree, boolean hasBody){
        int argsCount = 4 + (hasChunkNumber? 1 : 0) + (hasReplicationDegree? 1 : 0);

        int endHeader = 3;
        while (message[endHeader - 3] != 0xD || message[endHeader - 2] != 0xA ||
                message[endHeader - 1] != 0xD || message[endHeader] != 0xA)
            endHeader++;


        String header = new String(this.message, 0, endHeader-3, StandardCharsets.US_ASCII);

        String[] arguments = header.split(" ");
        
        if (argsCount != arguments.length){
            System.out.println("Error parsing message: wrong number of arguments");
            System.out.println("Expected " + argsCount + " got " + arguments.length);
        }

        List<String> argsList = new ArrayList<>(Arrays.asList(arguments));

        
        if(hasBody){
            String bodyString = new String(this.message, endHeader+1,
                    this.message.length - (endHeader + 1), StandardCharsets.US_ASCII);
            argsList.add(bodyString);
        }

        return argsList;

    }
}
