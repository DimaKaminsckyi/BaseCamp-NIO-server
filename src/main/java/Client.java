
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Client implements Runnable {

    private final int port;
    private SocketChannel socketChannel;
    private SocketAddress socketAddress;
    private KeyGenerator keyGenerator;
    private String key;

    private Client(int port) throws IOException {
        this.port = port;
        this.socketAddress = new InetSocketAddress("localhost", port);
        this.socketChannel = SocketChannel.open(socketAddress);
        this.keyGenerator = new KeyGenerator();
        key = keyGenerator.generate();
    }

    private static volatile StringBuffer stringBuffer = new StringBuffer();
    private ByteBuffer buffer = ByteBuffer.allocate(50);


    @Override
    public void run() {

        Runnable runnable = () -> {
            String message = "";
            String idTo;
            do {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                try {
                    System.out.println("Id user/broadcast : ");
                    idTo = reader.readLine();
                    stringBuffer.append(idTo);

                    System.out.println("Message : ");
                    message = reader.readLine();

                    if (!idTo.contains("broadcast")){
                        stringBuffer.append(Encryption.XOR(message, key));
                        stringBuffer.insert(1, "has0|");
                    }
                    else
                        stringBuffer.append(message);
                    writeMessageToTheServer();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (!message.equals("/close"));
        };
        Thread thread = new Thread(runnable);
        thread.start();


        try {
            while (true) {
                buffer.clear();
                socketChannel.read(buffer);
                stringBuffer.append(new String(getByteArrayFromByteBuffer(buffer)));
                if (Character.isDigit(stringBuffer.charAt(0))){
                    phaseCheck();
                }else{
                    System.out.println(stringBuffer);
                }
                stringBuffer.setLength(0);
                buffer.clear();

            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void writeMessageToTheServer() {

        try {
            byte[] bytes = stringBuffer.toString().getBytes();
            buffer.clear();
            buffer.put(bytes);
            buffer.flip();
            socketChannel.write(buffer);
            buffer.clear();
            stringBuffer.setLength(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] getByteArrayFromByteBuffer(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.position()];
        byteBuffer.rewind();
        byteBuffer.get(bytes);
        return bytes;
    }

    private void phaseHandler(char phase , String message , String infoMessage){
        message = Encryption.XOR(message , key);
        phase++;
        stringBuffer.setLength(0);
        stringBuffer.append(message);
        stringBuffer.insert(0 , infoMessage + phase + "|");
        stringBuffer.setCharAt(0 , infoMessage.charAt(2));
        writeMessageToTheServer();
    }

    private void phaseCheck(){
        char phase = stringBuffer.charAt(4);
        String infoMessage = stringBuffer.substring(0 , 4);
        String message = stringBuffer.substring(6 ,stringBuffer.length());
        switch (phase){
            case '0' :
                phaseHandler(phase , message , infoMessage);
                break;
            case '1':
                phaseHandler(phase , message , infoMessage);
                break;
            case '2':
                message = Encryption.XOR(message , key);
                System.out.println("Final Message : " + message);
                break;
            default:
                System.out.println("Phase not found !");
        }
    }


    public static void main(String[] args) throws IOException {
        Client client = new Client(8888);
        client.run();
    }

}
