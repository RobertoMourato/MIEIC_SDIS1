import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MessageHandler implements Runnable {
    Peer peer;
    byte[] message;

    MessageHandler(Peer peer, byte[] message) {
        this.peer = peer;
        this.message = message;
    }

    @Override
    public void run() {
        String mes = new String(this.message, StandardCharsets.US_ASCII);
        mes = mes.trim();
        String[] parameters = mes.split(" ");
        if (peer.getPeerId() == Integer.parseInt(parameters[2])) {
            return;
        }
        switch (parameters[1]) {
            case "PUTCHUNK":
                System.out.println("PUTCHUNK" + this.peer.getPeerId());
                handlePutChunk();
                break;
            case "STORED":
                System.out.println("STORED" + this.peer.getPeerId());
                handleStored();
                break;
            case "GETCHUNK":
                System.out.println("GETCHUNK" + this.peer.getPeerId());
                handleGetChunk();
                break;
            case "CHUNK":
                System.out.println("CHUNK" + this.peer.getPeerId());
                handleChunk();
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

        List<String> arguments = parseMessage(true, true);

        String fileName = arguments.get(3) + "_" + arguments.get(4);

        this.peer.getStorage().getStoredChunksOccurrences().put(fileName, 0);

        String filePath = peer.getPeerId() + "/" + fileName;
        File tmp = new File(filePath);
        tmp.getParentFile().mkdirs();

        try {
            tmp.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] body = parseBody();

        Chunk receivedChunk = new Chunk(arguments.get(3), Integer.parseInt(arguments.get(4)),
                Integer.parseInt(arguments.get(5)), body.length);

        this.peer.getStorage().getStoredChunks().put(receivedChunk.getIdentifier(), receivedChunk);

//        this.peer.getStorage().getStoredChunks().add(new Chunk(arguments.get(3),
//                Integer.parseInt(arguments.get(4)), body, Integer.parseInt(arguments.get(5))));

        try {
            FileOutputStream writeToFile = new FileOutputStream(filePath);
            writeToFile.write(body);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String header = "1.0 STORED " + peer.getPeerId() + " " + arguments.get(3) + " " + arguments.get(4) + "\r\n\r\n";

        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * 400));
            this.peer.getControlChannel().sendMessage(header.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void handleStored() {
        List<String> arguments = parseMessage(true, false);

        String fileName = arguments.get(3) + "_" + arguments.get(4);

        this.peer.getStorage().getStoredChunksOccurrences().put(fileName,
                this.peer.getStorage().getStoredChunksOccurrences().get(fileName) + 1);

        System.out.println("Peer " + peer.getPeerId() + " Occur " + fileName + " " + this.peer.getStorage().getStoredChunksOccurrences().get(fileName));

    }

    void handleGetChunk() {
        List<String> arguments = parseMessage(true, false);

        String fileName = arguments.get(3) + "_" + arguments.get(4);
        this.peer.getStorage().getOtherPeersWantedChunks().put(fileName, true);

        boolean foundChunk = false;
        byte[] body = new byte[0];

        if (this.peer.getStorage().getStoredChunks().get(fileName) != null){
            Chunk chunk = this.peer.getStorage().getStoredChunks().get(fileName);
            foundChunk = true;
            try {
                body = Files.readAllBytes(Chunk.getFileChunk(this.peer.getPeerId(), chunk.getIdentifier()).toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
//            System.arraycopy(chunk.getContent(), 0, body, 0, body.length);
        }

//        for (Chunk chunk : this.peer.getStorage().getStoredChunks()) {
//            if (chunk.getFileId().equals(arguments.get(3)) &&
//                    chunk.getChunkNo() == Integer.parseInt(arguments.get(4))) {
//                foundChunk = true;
//                body = new byte[chunk.getContent().length];
//                System.arraycopy(chunk.getContent(), 0, body, 0, body.length);
//                break;
//            }
//        }

        if (foundChunk) {
            String header = "1.0 CHUNK " + this.peer.getPeerId() + " " + arguments.get(3) + " " + arguments.get(4) + "\r\n\r\n";
            byte[] encodedHeader = header.getBytes(StandardCharsets.US_ASCII);
            byte[] message = new byte[encodedHeader.length + body.length];
            System.arraycopy(encodedHeader, 0, message, 0, encodedHeader.length);
            System.arraycopy(body, 0, message, encodedHeader.length, body.length);

            try {
                TimeUnit.MILLISECONDS.sleep((long) (Math.random() * 400));
                if (this.peer.getStorage().getOtherPeersWantedChunks().get(fileName))
                    this.peer.getRestoreChannel().sendMessage(message);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.peer.getStorage().getOtherPeersWantedChunks().put(fileName, false);
        }
    }

    void handleChunk() {
        List<String> arguments = parseMessage(true, false);

        String fileName = arguments.get(3) + "_" + arguments.get(4);
        this.peer.getStorage().getOtherPeersWantedChunks().put(fileName, false);


        if (this.peer.getStorage().getSelfPeerWantedChunks().get(fileName)){
            String filePath = peer.getPeerId() + "/wanted/" + fileName;
            File tmp = new File(filePath);
            tmp.getParentFile().mkdirs();

            this.peer.getStorage().getSelfPeerWantedChunks().put(fileName, false);
            this.peer.getStorage().getStoredSelfWantedChunks().put(fileName, true);

            try {
                tmp.createNewFile();
                FileOutputStream writeToFile = new FileOutputStream(tmp);
                writeToFile.write(parseBody());
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

//        // penso que depois disto, na função de restore mesmo no peer, dps tenha de chamar uma função que reconstroi o ficheiro com as chunks que recebeu, mas tenho de ver melhor amanha
//        for (int i = 0; i < this.peer.getStorage().getFilesData().size(); i++) {
//            if(this.peer.getStorage().getFilesData().get(i).getFileId().equals(arguments.get(3))){
//                this.peer.getStorage().getFilesData().get(i).getChunks().get(Integer.parseInt(arguments.get(4))).setContent(arguments.get(5).getBytes(StandardCharsets.US_ASCII));
//            }
//        }

    }

    List<String> parseMessage(boolean hasChunkNumber, boolean hasReplicationDegree) {
        int argsCount = 4 + (hasChunkNumber ? 1 : 0) + (hasReplicationDegree ? 1 : 0);

        int endHeader = 3;
        while (message[endHeader - 3] != 0xD || message[endHeader - 2] != 0xA ||
                message[endHeader - 1] != 0xD || message[endHeader] != 0xA)
            endHeader++;


        String header = new String(this.message, 0, endHeader - 3, StandardCharsets.US_ASCII);

        String[] arguments = header.split(" ");

        if (argsCount != arguments.length) {
            System.out.println("Error parsing message: wrong number of arguments");
            System.out.println("Expected " + argsCount + " got " + arguments.length);
        }

        //
//
//        if (hasBody) {
//            String bodyString = new String(this.message, endHeader + 1,
//                    this.message.length - (endHeader + 1), StandardCharsets.US_ASCII);
//            argsList.add(bodyString);
//        }

        return new ArrayList<>(Arrays.asList(arguments));
    }

    public byte[] parseBody() {

        int endHeader = 3;
        while (message[endHeader - 3] != 0xD || message[endHeader - 2] != 0xA ||
                message[endHeader - 1] != 0xD || message[endHeader] != 0xA)
            endHeader++;

        byte[] body = new byte[this.message.length - (endHeader + 1)];

        System.arraycopy(message, endHeader + 1, body, 0, body.length);

        return body;
    }
}
