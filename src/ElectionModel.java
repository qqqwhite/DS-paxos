import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ElectionModel {

    List<AL_Role> alRoles = new ArrayList<>();
    List<PAL_Role> palRoles = new ArrayList<>();
    Map<String, String> nameIpPorts = new HashMap<>();

    // 8001 8002 8003 | 8004 - 8009
    public ElectionModel() {
        nameIpPorts.put("M1", "localhost:8001");
        nameIpPorts.put("M2", "localhost:8002");
        nameIpPorts.put("M3", "localhost:8003");
        nameIpPorts.put("M4", "localhost:8004");
        nameIpPorts.put("M5", "localhost:8005");
        nameIpPorts.put("M6", "localhost:8006");
        nameIpPorts.put("M7", "localhost:8007");
        nameIpPorts.put("M8", "localhost:8008");
        nameIpPorts.put("M9", "localhost:8009");
        for (int i = 4; i<=9;i++){
            String name = "M"+i;
            int port = Integer.parseInt(nameIpPorts.get(name).split(":")[1]);
            AL_Role alRole = null;
            try {
                alRole = new AL_Role(port, name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            alRoles.add(alRole);
        }
        for (int i = 1; i<=3;i++){
            String name = "M"+i;
            int port = Integer.parseInt(nameIpPorts.get(name).split(":")[1]);
            PAL_Role palRole = null;
            try {
                palRole = new PAL_Role(port, name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            palRoles.add(palRole);

        }
    }


    public void setRandomReplyDelay(int delay){
        PAL_Role palRole_M2 = this.palRoles.get(1);
        assert palRole_M2.name.equals("M2");
        palRole_M2.setReplyDelay(delay);
        for (AL_Role alRole:this.alRoles){
            alRole.setReplyDelay(delay);
        }
    }

    public void setNoReceive(String name){
        Server server = getTargetCandidate(name);
        assert server != null;
        server.setCanReceive(false);
    }

    public void beginToReceive(String name){
        Server server = getTargetCandidate(name);
        assert server != null;
        server.setCanReceive(true);
    }

    public void setReplyDelay(int delay, String name){
        Server server = getTargetCandidate(name);
        assert server != null;
        server.setReplyDelay(delay);
    }

    private Server getTargetCandidate(String name){
        for (PAL_Role palRole:this.palRoles){
            if (palRole.name.equals(name)){
                return palRole;
            }
        }
        for (AL_Role alRole:this.alRoles){
            if (alRole.name.equals(name)){
                return alRole;
            }
        }
        return null;
    }

    public void startElectionAndDisconnect(){
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (PAL_Role palRole : palRoles) {
            executorService.submit(() -> {
                if (palRole.name.equals("M1")) {
                    palRole.startElection(nameIpPorts);
                }else {
                    palRole.startElectionAndDisconnect(nameIpPorts);
                }
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Config.TIMEOUT * 999, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Map<String, Integer> votes = new HashMap<>();
        for (AL_Role alRole : alRoles){
            alRole.shutdown();
            int count = votes.getOrDefault(alRole.getPresident(), 0);
            votes.put(alRole.getPresident(), count + 1);
        }
        for (PAL_Role palRole : palRoles){
            palRole.shutdown();
            int count = votes.getOrDefault(palRole.getPresident(), 0);
            votes.put(palRole.getPresident(), count + 1);
        }
        votes.remove(null);
        System.out.println(votes.keySet().iterator().next() + " wins!");
    }

    public void startElection() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (PAL_Role palRole : palRoles) {
            executorService.submit(() -> {
                palRole.startElection(nameIpPorts);
            });
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Config.TIMEOUT * 999, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Map<String, Integer> votes = new HashMap<>();
        for (AL_Role alRole : alRoles){
            alRole.shutdown();
            int count = votes.getOrDefault(alRole.getPresident(), 0);
            votes.put(alRole.getPresident(), count + 1);
        }
        for (PAL_Role palRole : palRoles){
            palRole.shutdown();
            int count = votes.getOrDefault(palRole.getPresident(), 0);
            votes.put(palRole.getPresident(), count + 1);
        }
        System.out.println(votes.keySet().iterator().next() + " wins!");
    }

    public static void main(String[] args) {
        ElectionModel electionModel = new ElectionModel();
        electionModel.startElection();
    }

}
