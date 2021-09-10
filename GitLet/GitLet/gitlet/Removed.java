package gitlet;

import java.io.Serializable;
import java.util.ArrayList;

public class Removed implements Serializable {

    private ArrayList<String> lst;

    /** construct a removed object*/
    public Removed() {
        lst = new ArrayList<>();

    }

    /** add a removed object with its name */
    public void add(String name) {
        lst.add(name);
    }

    /** return all the currently removed objects*/
    public ArrayList<String> get() {
        return lst;
    }

    /** remove and remove obj from the rm_lst*/
    public void delete(String name) {
        lst.remove(name);
    }

    public boolean cancommit() {
        return lst.size() > 0;
    }

    public void clearRmLst() {
        lst.clear();
    }
}
