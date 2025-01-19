import org.junit.Test;
import static org.junit.Assert.*;

public class ElectionModel_test {
    private ElectionModel electionModel;

    @Test
    public void basicTest(){
        electionModel = new ElectionModel();
        electionModel.startElection();
        assertTrue(checkPresident(electionModel));
    }

    @Test
    public void replyRandomSmailDelayTest(){
        electionModel = new ElectionModel();
        electionModel.setRandomReplyDelay(1000);
        electionModel.startElection();
        assertTrue(checkPresident(electionModel));
    }

    @Test
    public void replyRandomM2HugeDelayTest(){
        electionModel = new ElectionModel();
        electionModel.setRandomReplyDelay(1000);
        electionModel.setReplyDelay(9999, "M2");
        electionModel.startElection();
        assertTrue(checkPresident(electionModel));
    }

    @Test
    public void replyRandomHugeDelayTest(){
        electionModel = new ElectionModel();
        electionModel.setRandomReplyDelay(4000);
        electionModel.startElection();
        assertTrue(checkPresident(electionModel));
    }

    @Test
    public void replyRandomDelayAndNoReceiveTest() {
        electionModel = new ElectionModel();
        electionModel.setRandomReplyDelay(4000);
        electionModel.setNoReceive("M3");
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                electionModel.beginToReceive("M3");
            }
        }.start();
        electionModel.startElection();
        assertTrue(checkPresident(electionModel));
    }

    @Test
    public void disconnectionTest(){
        electionModel = new ElectionModel();
        electionModel.startElectionAndDisconnect();
        assertTrue(checkPresident(electionModel));
    }



    public static boolean checkPresident(ElectionModel electionModel){
        String targetPresident = electionModel.palRoles.get(0).getPresident();
        for (AL_Role alRole: electionModel.alRoles){
            if (!alRole.getPresident().equals(targetPresident)){
                return false;
            }
        }
        for (PAL_Role palRole: electionModel.palRoles){
            if (palRole.selector.isOpen() && !palRole.getPresident().equals(targetPresident)){
                return false;
            }
        }
        return true;
    }


}
