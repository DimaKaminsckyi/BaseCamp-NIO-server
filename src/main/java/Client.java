
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

    private Client(int port) throws IOException {
        this.port = port;
        this.socketAddress = new InetSocketAddress("localhost", port);
        this.socketChannel = SocketChannel.open(socketAddress);
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
                    System.out.println("Id user : ");
                    idTo = reader.readLine();
                    stringBuffer.append(idTo);

                    System.out.println("Message : ");
                    message = reader.readLine();
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
                stringBuffer.setLength(0);
                buffer.clear();
                socketChannel.read(buffer);
                stringBuffer.append(new String(getByteArrayFromByteBuffer(buffer)));
                System.out.println(stringBuffer);
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


    public static void main(String[] args) throws IOException {
        Client client = new Client(8888);
        client.run();
    }

}
