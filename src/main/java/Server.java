
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import org.apache.log4j.Logger;

public class Server implements Runnable {

    private static final Logger log = Logger.getLogger(Server.class);

    private final int port;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private HashMap<String, SocketChannel> socketId = new HashMap<>();
    private HashMap<String, String> messageBox = new HashMap<>();
    private int count = 0;
    private String idTo;
    private String idFrom;


    private Server(int port) throws IOException {
        this.port = port;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress("localhost", port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();

        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }


    @Override
    public void run() {
        log.info("Server starting in " + this.port + " port");

        try {
            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();

                    if (selectionKey.isAcceptable()) {
                        handleAcceptableKeys();
                    } else {
                        if (selectionKey.isReadable()) {
                            handleReadKey(selectionKey);
                        } else if (selectionKey.isWritable()) {
                            handleWriteKey(selectionKey);
                        }
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }


    private void handleAcceptableKeys() throws IOException {

        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        String id = generateId();
        socketId.put(id, socketChannel);
        log.info("Connection Accepted : " + id + " " + socketChannel.getRemoteAddress() );
    }


    private void handleReadKey(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(50);

        StringBuilder sb = new StringBuilder();
        byteBuffer.clear();
        try {
            while ((socketChannel.read(byteBuffer)) > 0) {
                byteBuffer.flip();
                byte[] bytes = new byte[byteBuffer.limit()];
                byteBuffer.get(bytes);
                sb.append(new String(bytes));
                byteBuffer.clear();
            }

            idFrom = getUserId(socketChannel);

            if (Character.isDigit(sb.charAt(0))){
                idTo = sb.substring(0, 1);
                messageBox.put(idTo, sb.substring(4));
                if (key.isValid()){key.interestOps(SelectionKey.OP_WRITE);}
            }
            else if (sb.toString().contains("broadcast")){
                messageBox.put(idFrom , sb.substring(9));
                broadcast("From " + idFrom + " " + messageBox.get(idFrom));
            }

        } catch (IOException e) {
            log.info(idTo + " left the chat.\n");
            socketChannel.close();
        }
    }



    private void handleWriteKey(SelectionKey key) throws IOException {
        SocketChannel socketChannelTo = socketId.get(idTo);

        String s = "";

        if (!(socketId.get(idTo)==null)&& socketId.get(idTo).getRemoteAddress().toString().equals(socketChannelTo.getRemoteAddress().toString())) {
            s = idTo + "|" + idFrom + "|" + messageBox.get(idTo);
            log.info("writing to " + idTo + " user " + s);
            messageBox.remove(idTo);

        }

        byte[] bytes;
        bytes = s.getBytes();
        socketChannelTo.write(ByteBuffer.wrap(bytes));
        key.interestOps(SelectionKey.OP_READ);
    }

    private void broadcast(String msg) throws IOException {
        ByteBuffer msgBuf=ByteBuffer.wrap(msg.getBytes());
        for(SelectionKey key : selector.keys()) {
            if(key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel sch=(SocketChannel) key.channel();
                sch.write(msgBuf);
                idTo = getUserId(sch);
                log.info("From " + idFrom + " to " + idTo + " " + msg);
                msgBuf.rewind();
            }
        }
    }

    private String generateId() {
        count++;
        return String.valueOf(count);
    }

    private String getUserId(SocketChannel socketChannel) {
        String key = "";
        Set<Map.Entry<String, SocketChannel>> entrySet=socketId.entrySet();
        for (Map.Entry<String, SocketChannel> pair : entrySet) {
            if (socketChannel.equals(pair.getValue())) {
                key = pair.getKey();
            }
        }
        return key;
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server(8888);
        server.run();
    }
}
