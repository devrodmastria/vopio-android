package info.vopio.captions.DataModel;

public class MessageModel {

    private String id;
    private String text;
    private String name;

    public MessageModel(){}

    public MessageModel(String text, String name, String photoUrl, String imageUrl) {
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


}
