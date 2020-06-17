package info.vopio.android.DataModel;

public class MessageModel {

    private String id;
    private String text;
    private String name;

    public MessageModel(){}

    public MessageModel(String text, String name) {
        this.text = text;
        this.name = name;
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


}
