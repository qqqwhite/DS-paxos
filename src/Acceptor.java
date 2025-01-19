import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface Acceptor {

    /**
     * deal with message type 1
     * @param msg
     * @param socketChannel
     * @throws IOException
     */
    public void acceptorHandler1(Message msg, SocketChannel socketChannel) throws IOException;

    /**
     * deal with message type 2
     * @param msg
     * @param socketChannel
     * @throws IOException
     */
    public void acceptorHandler2(Message msg, SocketChannel socketChannel) throws IOException;

}
