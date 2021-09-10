package gitlet;

import java.io.Serializable;
import java.util.HashMap;

public class MyBranch implements Serializable {

    private HashMap<String, String> branchmap; /**mapping from the branch name to the commitid*/
    private String curr;
    private String reset;

    /** Construct a branch object*/
    public MyBranch() {
        this.branchmap = new HashMap<>();
        this.curr = null;
        this.reset = null;
    }

    /** Retyrn the branch map.*/
    public HashMap<String, String> getMap() {
        return this.branchmap;
    }

    /** Add a branch with head and commitid. And set the branch to be the current branch.*/
    public void add(String head, String commitid) {
        branchmap.put(head, commitid);
        curr = head;
    }

    /** Put a branch with branch_name and commitid.*/
    public void put(String branchname, String commitid) {
        branchmap.put(branchname, commitid);
    }

    /** remove branchname.*/
    public void removebranch(String branchname) {
        branchmap.remove(branchname);
    }

    /** return the head commit*/
    public String getcommit(String head) {
        return branchmap.get(head);
    }

    /** return the current commit*/
    public String getCurrentcommit() {
        return branchmap.get(curr);
    }

    /** get the current branch with curr*/
    public String getCurrent() {
        return curr;
    }

    /** change current with input.*/
    public void change(String input) {
        this.curr = input;
    }

    /** Set reset with branchname.*/
    public void setReset(String branchname) {
        this.reset = branchname;
    }

    /** Reset*/
    public void reset() {
        this.branchmap.put(curr, reset);
        this.reset = null;
    }
}
