import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class Server {

    final String name;

    private volatile boolean canReceive = true;

    private int replyDelay = 0;

    protected Selector selector = Selector.open();

    // 创建一个固定大小的线程池
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10); // 可以根据需要调整线程数

    public boolean isCanReceive() {
        return canReceive;
    }

    public void setCanReceive(boolean canReceive) {
        this.canReceive = canReceive;
    }

    public int getReplyDelay() {
        return replyDelay;
    }

    public void setReplyDelay(int replyDelay) {
        this.replyDelay = replyDelay;
    }

    public Server(int port, String name) throws IOException {
        this.name = name;
        new Thread() {
            @Override
            public void run() {
                try {
                    server(port);
                } catch (IOException e) {
                    System.err.println("server " + name + " shutdown");
                }
            }
        }.start();
    }

    private void server(int port) throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        // waiting for lamport time info
        while (true) {
            selector.select();
            if (!selector.isOpen()) break;
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while (keyIterator.hasNext()) {
                if (!selector.isOpen()) break;
                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                if (key.isAcceptable()) {
                    // build a new connection
                    ServerSocketChannel socketChannel = (ServerSocketChannel) key.channel();
                    SocketChannel channel = socketChannel.accept();
                    channel.configureBlocking(false);
                    if (!selector.isOpen()) break;
                    channel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    // deal with info
                    handleRead(key);
                }
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(Config.BUFFER_SIZE);
        int bytesRead = socketChannel.read(byteBuffer);
        if (bytesRead == -1) {
            socketChannel.close();
            return;
        }
        byteBuffer.flip();
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteBuffer.array());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Message message = (Message) objectInputStream.readObject();
            if (Config.DEBUG) System.out.println(name + " receive: " + message.toString());
            if (message.getStep() == 0){
                handleMsg(message, socketChannel);
            }else {
                beforeReceiveMsg();
                threadPool.submit(() -> {
                    try {
                        handleMsg(message, socketChannel);
                    } catch (IOException e) {
                        System.err.println(name + " error handling message: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (ClassNotFoundException e) {
            if (Config.DEBUG) System.err.println(name + " failed to deserialize message");
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void handleMsg(Message msg, SocketChannel socketChannel) throws IOException {}

    // 延迟收到
    protected void beforeReceiveMsg() throws InterruptedException {
        while (!canReceive){
            Thread.yield();
        }
    }

    // 延迟回复
    protected void beforeReply() throws InterruptedException {
        Thread.sleep((long) (replyDelay * Math.random()));
    }

    protected List<Message> sendAndReceiveMsgs(Message msg, Map<String, String> nameIpPorts, int count, boolean isReply) {
        ExecutorService executorService = Executors.newFixedThreadPool(nameIpPorts.size());
        List<Message> messages = new ArrayList<>();
        for (Map.Entry<String, String> entry : nameIpPorts.entrySet()) {
            executorService.submit(() -> {
                Message receiveMsg = null;
                try {
                    SocketChannel socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(true);
                    socketChannel.connect(new InetSocketAddress(entry.getValue().split(":")[0], Integer.parseInt(entry.getValue().split(":")[1])));
                    while (!socketChannel.finishConnect()) {
                        Thread.yield();
                    }
                    sendMsg(msg, socketChannel, isReply);
                    receiveMsg = receiveMsg(socketChannel);
                }catch (IOException e) {
                    if (Config.DEBUG) System.err.println(name + " failed to send message");
                }
                messages.add(receiveMsg);
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Config.TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        while (messages.size() < count) messages.add(null);
        // 可能会需要对executor做一个超时兜底策略
        return messages;
    }

    protected void sendMsgs(Message msg, Map<String, String> nameIpPorts, boolean isReply) {
        ExecutorService executorService = Executors.newFixedThreadPool(nameIpPorts.size());
        for (Map.Entry<String, String> entry : nameIpPorts.entrySet()) {
            executorService.submit(() -> {
                try {
                    SocketChannel socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(true);
                    socketChannel.connect(new InetSocketAddress(entry.getValue().split(":")[0], Integer.parseInt(entry.getValue().split(":")[1])));
                    while (!socketChannel.finishConnect()) {
                        Thread.yield();
                    }
                    sendMsg(msg, socketChannel, isReply);
                }catch (IOException e) {
                    if (Config.DEBUG) System.err.println(name + " failed to send message");
                }
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Config.TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected Message sendAndReceiveMsg(Message msg, String ip, int port, boolean isReply) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.socket().setSoTimeout(Config.TIMEOUT);
        socketChannel.connect(new InetSocketAddress(ip, port));
        while (!socketChannel.finishConnect()){
            Thread.yield();
        }
        sendMsg(msg, socketChannel, isReply);
        return receiveMsg(socketChannel);
    }

    protected Message receiveMsg(SocketChannel socketChannel) throws IOException {
        try {
            beforeReceiveMsg();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(Config.BUFFER_SIZE);
        int bytesRead = socketChannel.read(byteBuffer);
        if (bytesRead == -1) {
            socketChannel.close();
            return null;
        }
        byteBuffer.flip();
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteBuffer.array());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Message message = (Message) objectInputStream.readObject();
            if (Config.DEBUG) System.out.println(name + " receive: " + message.toString());
            return message;
        } catch (ClassNotFoundException e) {
            if (Config.DEBUG) System.err.println(name + " failed to deserialize message");
            e.printStackTrace();
        }
        return null;
    }

    protected void sendMsg(Message msg, SocketChannel socketChannel, boolean isReply) throws IOException {
        if (isReply) {
            try {
                beforeReply();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(msg);
        objectOutputStream.flush();

        byte[] bytes = byteArrayOutputStream.toByteArray();
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        while (byteBuffer.hasRemaining()) {
            socketChannel.write(byteBuffer);
        }
        if (Config.DEBUG) System.out.println(name + " send: " + msg.toString());
        objectOutputStream.close();
        byteArrayOutputStream.close();
    }

    // 添加一个关闭方法来正确地关闭线程池
    public void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }
}
