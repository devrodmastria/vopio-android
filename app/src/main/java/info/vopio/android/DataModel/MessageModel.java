package info.vopio.android.DataModel;

public class MessageModel {

    private String id;
    private String text;
    private String name;
    private String feedback;

    public MessageModel(){}

    public MessageModel(String text, String name) {
        this.text = text;
        this.name = name;
        this.feedback = "n/a";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFeedback() { return feedback; }

    public void setFeedback(String feedback) {this.feedback = feedback;}

}
