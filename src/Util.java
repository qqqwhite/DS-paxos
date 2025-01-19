import java.util.List;
import java.util.Map;

public class Util {

    public static int getMaxId(List<Message> msgs){
        int maxId = 0;
        for(Message msg : msgs){
            if (msg != null){
                maxId = Math.max(maxId, msg.getId());
            }
        }
        return maxId;
    }

    public static boolean checkHalfVote(List<Message> msgs) {
        int count = 0;
        for (Message msg : msgs) {
            if (msg != null && msg.getMessage().equals(Config.PASS)){
                count++;
            }
        }
        return count > (msgs.size() / 2);
    }

    public static String getVoteRate(List<Message> msgs) {
        int count = 0;
        for (Message msg : msgs) {
            if (msg != null && msg.getMessage().equals(Config.PASS)){
                count++;
            }
        }
        return "voting: (pass) " + count + " : (reject) " + (msgs.size() - count);
    }

    public static boolean checkEnd(List<Message> msgs) {
//        for (Message msg: msgs){
//            if (msg != null && msg.getMessage().equals(Config.END)){
//                return true;
//            }
//        }
        return false;
    }
}
