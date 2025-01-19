import java.io.IOException;
import java.nio.channels.SocketChannel;

public class AL_Role extends Server implements Acceptor, Learner {

    public static String role = "AL";

    // acceptor
    private Integer prepareId = -1;

    private Integer acceptId = -1;

    // learner
    private Message result = null;

    public AL_Role(int port, String name) throws IOException {
        super(port, name);
    }

    @Override
    protected void handleMsg(Message msg, SocketChannel socketChannel) throws IOException {
        switch (msg.getStep()) {
            case 0:
                Message sendMsg = new Message(0, -1, role);
                sendMsg(sendMsg, socketChannel, false);
                break;
            case 1: // come from proposer 1
                acceptorHandler1(msg, socketChannel);
                break;
            case 4:
                acceptorHandler2(msg, socketChannel);
                break;
            case 7:
                leanerHandler(msg);
                break;
        }
    }

    @Override
    public void acceptorHandler1(Message msg, SocketChannel socketChannel) throws IOException {
//        if (this.result != null){
//            synchronized (this.result){
//                Message sendMsg = new Message(msg.getStep()+1, -1, Config.END);
//                sendMsg(sendMsg, socketChannel);
//                return;
//            }
//        }
        // 尝试通过id请求
        if (this.prepareId < msg.getId()){
            Message sendMsg = new Message(msg.getStep()+1, msg.getId(), Config.PASS);
            sendMsg(sendMsg, socketChannel, true);
        }else {
            Message sendMsg = new Message(msg.getStep()+1, this.prepareId, Config.REJECT);
            sendMsg(sendMsg, socketChannel, true);
            return;
        }
        // 防止后续非正常请求通过
        synchronized (this.prepareId) {
            this.prepareId = msg.getId();
        }
    }

    @Override
    public void acceptorHandler2(Message msg, SocketChannel socketChannel) throws IOException {
//        if (this.result != null){
//            synchronized (this.result){
//                Message sendMsg = new Message(msg.getStep()+1, -1, Config.END);
//                sendMsg(sendMsg, socketChannel);
//                return;
//            }
//        }
        if (msg.getId() < this.prepareId || msg.getId() <= this.acceptId){
            Message sendMsg = new Message(msg.getStep()+1, Math.max(this.prepareId, this.acceptId), Config.REJECT);
            sendMsg(sendMsg, socketChannel, true);
            return;
        }else {
            Message sendMsg = new Message(msg.getStep()+1, msg.getId(), Config.PASS);
            sendMsg(sendMsg, socketChannel, true);
        }
        synchronized (this.acceptId) {
            this.acceptId = msg.getId();
        }
    }

    @Override
    public void leanerHandler(Message msg) {
        if (this.result == null) {
            this.result = msg;
            return;
        }
        synchronized (this.result) {
            if (this.result.getId() > msg.getId()) {
                this.result = msg;
            }
        }
    }

    @Override
    public String getPresident() {
        if (result == null) return null;
        return result.getMessage();
    }
}
