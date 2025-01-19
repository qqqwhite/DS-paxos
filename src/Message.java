import java.io.Serializable;

public class Message implements Serializable {

    private final int step;

    private final int id;

    private final String message;

    public Message(int step, int id, String message) {
        this.step = step;
        this.id = id;
        this.message = message;
    }

    public Message(int step, int id) {
        this.step = step;
        this.id = id;
        this.message = null;
    }

    public int getStep() {
        return step;
    }

    public int getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Step: ").append(step).append(";");
        sb.append("Id: ").append(id).append(";");
        if (message != null) {
            sb.append("Message: ").append(message).append("\n");
        }else {
            sb.append("Message is null\n");
        }
        return sb.toString();
    }
}
