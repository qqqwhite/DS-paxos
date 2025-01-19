import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PAL_Role extends Server implements Proposer, Acceptor, Learner{

    public static String role = "PAL";

    Map<String, String> acceptorIpPorts = new HashMap<>();

    Map<String, String> learnerIpPorts = new HashMap<>();

    // proposer
    private int id = 0;

    // acceptor
    private Integer prepareId = -1;

    private Integer acceptId = -1;

    // learner
    private Message result = null;
    /**
     *
     * @param port
     * @param name
     */
    public PAL_Role(int port, String name) throws IOException {
        super(port, name);
    }

    public void startElection(Map<String, String> nameIpPorts) {
        Map<String, String> copyNameIpPorts = new HashMap<>(nameIpPorts);
        healthCheck(copyNameIpPorts);
        initProposal();
    }

    public void startElectionAndDisconnect(Map<String, String> nameIpPorts) {
        Map<String, String> copyNameIpPorts = new HashMap<>(nameIpPorts);
        healthCheck(copyNameIpPorts);
        Message msg = new Message(1, this.id, null);
        sendMsgs(msg, nameIpPorts, false);
        try {
            this.selector.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * send 0:-1:health
     * @param nameIpPorts
     */
    private void healthCheck(Map<String, String> nameIpPorts) {
        Message msg = new Message(0, -1, "health");
        while (!nameIpPorts.isEmpty()) {
            List<String> healthyServerName = new ArrayList<>();
            for (Map.Entry<String, String> entry : nameIpPorts.entrySet()) {
                String ip = entry.getValue().split(":")[0];
                int port = Integer.parseInt(entry.getValue().split(":")[1]);
                Message receiveMsg = null;
                try {
                    receiveMsg = sendAndReceiveMsg(msg, ip, port, false);
                }catch (SocketTimeoutException e){
                    if (Config.DEBUG) System.err.println("connect to "+entry.getKey()+" timeout");
                }catch (IOException e){
                    if (Config.DEBUG) System.err.println(entry.getKey() + " receive failed");
                }
                if (receiveMsg != null && receiveMsg.getStep() == 0) {
                    healthyServerName.add(entry.getKey());
                    handleHealthCheck(receiveMsg, entry.getKey(), entry.getValue());
                }else {
                    if (Config.DEBUG) System.err.println(entry.getKey() + " healthy check failed");
                }
            }
            for (String serverName : healthyServerName) {
                nameIpPorts.remove(serverName);
                if (Config.DEBUG) System.out.println(serverName + " healthy check successful");
            }
        }
        System.out.println(this.name + " complete health check");
    }

    private void handleHealthCheck(Message msg, String serverName, String ipPort) {
        String role = msg.getMessage();
        for (char c:role.toCharArray()) {
            if (c == 'A'){
                this.acceptorIpPorts.put(serverName, ipPort);
            }else if (c == 'L'){
                this.learnerIpPorts.put(serverName, ipPort);
            }
        }
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

    @Override
    public void initProposal() {
        List<Message> receiveMsgs = null;
        while (true) {
            while (true) {
                Message msg = new Message(1, this.id, null);
                receiveMsgs = sendAndReceiveMsgs(msg, this.acceptorIpPorts, Config.ACCEPTOR_COUNT, false);
                if (Util.checkEnd(receiveMsgs)) return;
                if (Util.checkHalfVote(receiveMsgs)) {
                    break;
                } else {
                    System.out.println(this.name + " fail to prepare " + this.id + " with " + Util.getVoteRate(receiveMsgs));
                    this.id = Math.max(this.id, Util.getMaxId(receiveMsgs)) + 1;
                    try {
                        Thread.sleep((long) (Config.BASIC_WAITING_TIME * (1 + Math.random())));
                    } catch (InterruptedException e) {
                        if (Config.DEBUG) e.printStackTrace();
                    }
                }
            }
            System.out.println(this.name + " succeed to prepare " + this.id + " with " + Util.getVoteRate(receiveMsgs));
            Message msg = new Message(4, this.id, this.name);
            receiveMsgs = sendAndReceiveMsgs(msg, this.acceptorIpPorts, Config.ACCEPTOR_COUNT, false);
            if (Util.checkEnd(receiveMsgs)) return;
            if (Util.checkHalfVote(receiveMsgs)) {
                break;
            }else {
                System.out.println(this.name + " fail to accept " + this.id + " with " + Util.getVoteRate(receiveMsgs));
                this.id = Math.max(this.id, Util.getMaxId(receiveMsgs)) + 1;
                try {
                    Thread.sleep((long) (Config.BASIC_WAITING_TIME * (1 + Math.random())));
                } catch (InterruptedException e) {
                    if (Config.DEBUG) e.printStackTrace();
                }
            }
        }
        System.out.println(this.name + " succeed to accept " + this.id + " with " + Util.getVoteRate(receiveMsgs));
        Message msg = new Message(7, this.id, this.name);
        receiveMsgs = sendAndReceiveMsgs(msg, this.learnerIpPorts, Config.LEARNER_COUNT, false);
        System.out.println(this.name + " temporarily wins!");
    }
}
