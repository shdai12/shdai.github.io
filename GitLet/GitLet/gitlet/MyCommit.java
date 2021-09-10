package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class MyCommit implements Serializable {

    private Date date;
    private String parent;
    private String mergeparent;
    private String message;
    private HashMap<String, String> blobmap = new HashMap<>();

    /** A commit object with message and date*/
    MyCommit(String message, Date date) {
        this.date = date;
        this.message = message;
    }

    /** A commit object with messages, date, a parent, and a blob*/
    MyCommit(String message, Date date, String parent,
             HashMap<String, String> blobs) {
        this.date = date;
        this.message = message;
        this.blobmap = blobs;
    }

    /** construct a commit object with message, date, a parent, a merge parent, and a blob*/
    MyCommit(String message, Date date, String parent, String mergeparent,
             HashMap<String, String> blobs) {
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        this.date = date;
        this.parent = parent;
        this.mergeparent = mergeparent;
        this.message = message;
        this.blobmap = blobs;
    }


    public Date getDateTime() {
        return this.date;
    }

    public String getParent() {
        return this.parent;
    }

    public String getLog() {
        return this.message;
    }

    public HashMap<String, String> getBlobs() {
        return this.blobmap;
    }

    public boolean merged() {
        return this.mergeparent != null;
    }

    public String getmergeparent() {
        return this.mergeparent;
    }
}
