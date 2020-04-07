import java.nio.charset.StandardCharsets;

public class MessageHandler implements Runnable {
    byte[] message;

    MessageHandler(byte[] message){
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
}
