package gitlet;

import java.util.ArrayList;
import java.util.Set;

import java.util.Arrays;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.List;

import java.util.HashMap;
import java.util.Date;


public class User {

    public static File sarea = new File(".gitlet/staging_area");
    public static File workingdir = new File(".");
    public static File blobs = new File(".gitlet/blobs");
    public static File commit = new File(".gitlet/commits");
    public static File branches = new File(".gitlet/branches");
    public static File gitlet;

    /**
     * Initialize Gitlet
     */
    public void init() {
        gitlet = new File(".gitlet");
        if (gitlet.exists()) {
            System.out.print("A gitlet version-control system"
                    + "already exists in the current directory.");
            System.exit(0);
        }
        gitlet.mkdir();

        workingdir = new File(".");
        workingdir.mkdir();

        sarea = new File(".gitlet/staging_area");
        sarea.mkdir();

        Removed removed = new Removed();
        File removedfiled = new File(".gitlet/removed");
        Utils.writeObject(removedfiled, removed);

        blobs = new File(".gitlet/blobs");
        blobs.mkdir();

        commit = new File(".gitlet/commits");
        commit.mkdir();

        MyCommit initial = new MyCommit("initial commit", new Date(0));
        byte[] initialcommit = Utils.serializeObject(initial);
        String hashcode = Utils.sha1(initialcommit);

        MyBranch branchobj = new MyBranch();
        branches = new File(".gitlet/branches");
        branchobj.add("master", hashcode);
        Utils.writeObject(branches, branchobj);

        /** convert initial to a stream of bytes and store
         * them in Out whose name is stored in commitid*/
        File outFile = new File(".gitlet/commits/" + hashcode);
        try {
            ObjectOutputStream outs =
                    new ObjectOutputStream(new FileOutputStream(outFile));
            outs.writeObject(initial);
            outs.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*respectively stage the file to the staging area*/
    public void add(String[] filename) {
        if (filename.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        /**read contents of a file in the working directory*/
        File fileread = new File("./" + filename[1]);
        if (!fileread.exists()) {
            System.out.print("File does not exist.");
            System.exit(0);
        }
        /**handle files marked to be removed*/
        File removedf = new File(".gitlet/removed");
        Removed allremoved = Utils.readObject(removedf, Removed.class);
        allremoved.delete(filename[1]);
        Utils.writeObject(removedf, allremoved);

        byte[] filecontents = Utils.readContents(fileread);

        /**handle the identical case*/
        MyCommit currcom = currcommit();
        if (currcom.getBlobs().containsKey(filename[1])) {
            File blobf = new File(".gitlet/blobs/" + currcom.getBlobs().get(filename[1]));
            if (Arrays.equals(filecontents, Utils.readObject(blobf, Blob.class).getContents())) {
                return;
            }
        }
        /**Create a file if it is not existed, overwrite it otherwise*/
        File sfile = new File(".gitlet/staging_area/" + filename[1]);
        Utils.writeContents(sfile, filecontents);
    }

    /**
     * Create a commit
     */
    public void commit(String[] message) {
        if (message.length == 1) {
            System.out.print("Please enter a commit message.");
            System.exit(0);
        } else if (message.length != 2) {
            System.out.print("Incorrect operands.");
            System.exit(0);
        }

        /**Update the fileName map from the staging area and
         * parent commit and store the file in the different file folder respectively*/
        List<String> files = Utils.plainFilenamesIn(".gitlet/staging_area");
        Removed removedfile = Utils.readObject(new File(".gitlet/removed"), Removed.class);
        if (files.size() == 0 && !removedfile.cancommit()) {
            throw new IllegalArgumentException("No changes added to the commit.");
        }
        MyBranch branch = Utils.readObject(new File(".gitlet/branches"), MyBranch.class);
        String parentname = branch.getCurrentcommit();
        HashMap<String, String> blobsm = new HashMap<>();
        if (parentname != null) {
            MyCommit parentcommit = Utils.readObject(new File(".gitlet/commits/"
                    + parentname), MyCommit.class);
            for (String s : parentcommit.getBlobs().keySet()) {
                blobsm.put(s, parentcommit.getBlobs().get(s));
            }
        }
        List<String> currentremove = removedfile.get();

        /** clear the staging area*/
        for (String file : files) {
            File f = new File(".gitlet/staging_area/" + file);
            byte[] filecontents = Utils.readContents(f);
            Blob fileblob = new Blob(filecontents);

            String blobname = Utils.sha1(Utils.serializeObject(fileblob));
            File out = new File(".gitlet/blobs/" + blobname);
            f.delete();
            Utils.writeObject(out, fileblob);
            blobsm.put(file, blobname);
        }
        if (currentremove.size() > 0) {
            for (String removed : currentremove) {
                blobsm.remove(removed);
            }
        }
        removedfile.clearRmLst();
        Utils.writeObject(new File(".gitlet/removed"), removedfile);
        Date time = new Date(System.currentTimeMillis());

        MyCommit newcurrent = new MyCommit(message[1], time, parentname, null, blobsm);
        String commitname = Utils.sha1(Utils.serializeObject(newcurrent));
        branch.add(branch.getCurrent(), commitname);
        Utils.writeObject(new File(".gitlet/branches"), branch);
        Utils.writeObject(new File(".gitlet/commits/" + commitname), newcurrent);
    }


    /**
     * return the current commit
     */
    private MyCommit currcommit() {
        MyBranch branch = currbranch();
        String curr = branch.getCurrentcommit();
        File commitfile = new File(".gitlet/commits/" + curr);
        return Utils.readObject(commitfile, MyCommit.class);
    }

    /**
     * return the current branch
     */
    private MyBranch currbranch() {
        File branch1 = new File(".gitlet/branches");
        return Utils.readObject(branch1, MyBranch.class);
    }

    /* create a new branch with the branch command */
    public void branch(String[] branchname) {
        if (branchname.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        File branch1 = new File(".gitlet/branches");
        MyBranch b = Utils.readObject(branch1, MyBranch.class);
        if (b.getMap().containsKey(branchname[1])) {
            System.out.print("A Branch with that name already exists.");
            System.exit(0);
        }
        b.put(branchname[1], b.getCurrentcommit());
        Utils.writeObject(branch1, b);
    }

    public void selector(String[] args) {
        if (args.length == 2) {
            checkoutBranch(args, false);
        } else if (args.length == 3) {

            if (!args[1].equals("--")) {

                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            checkout(args);
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            checkoutf(args);
        } else {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }

    }

    public void checkout(String[] filename) {
        /* Write the entire contents of BYTES to FILE*/
        String currc = currbranch().getCurrentcommit();
        String id = filename[2];
        Set<String> fullIdSet = currcommit().getBlobs().keySet();
        if (id.length() >= 40 && !fullIdSet.contains(id)) {
            System.out.print("File does not exist in that commit.");
            System.exit(0);
        } else {
            boolean found = false;
            for (String fullId : fullIdSet) {
                if (fullId.substring(0, id.length()).equals(id)) {
                    found = true;
                    id = fullId;
                    break;
                }
            }
            if (!found) {
                System.out.print("File does not exist in that commit.");
                System.exit(0);
            }
        }
        File in = new File(".gitlet/blobs/" + currcommit().getBlobs().get(id));
        /* Overwrite files in the Working_dir */
        File f = new File("./" + id);
        Utils.writeContents(f, Utils.readObject(in, Blob.class).getContents());
        System.exit(0);
    }

    public void checkoutf(String[] idfilename) {
        String id = idfilename[1];
        String filename = idfilename[3];
        /* no id with the given commit exists */
        File commitf = new File(".gitlet/commits/" + id);
        if (id.length() < 40) {
            for (String fullId : Utils.plainFilenamesIn(".gitlet/commits/")) {
                if (fullId.substring(0, id.length()).equals(id)) {
                    commitf = new File(".gitlet/commits/" + fullId);
                    break;
                }
            }
        }
        if (!commitf.exists()) {
            System.out.print("No commit with that id exists.");
            System.exit(0);
        }
        /* get a commit from its sha1 id if the commit exists */
        MyCommit thiscom = Utils.readObject(commitf, MyCommit.class);
        if (!thiscom.getBlobs().containsKey(filename)) {
            System.out.print("File does not exist in that commit.");
            System.exit(0);
        }
        /* Overwrite files in the Working_dir */
        File f = new File("./" + filename);
        File in = new File(".gitlet/blobs/" + thiscom.getBlobs().get(filename));
        Utils.writeContents(f, Utils.readObject(in, Blob.class).getContents());
        System.exit(0);
    }

    public void checkoutBranch(String[] branchname, boolean reset) {
        MyBranch b = Utils.readObject(branches, MyBranch.class);
        HashMap<String, String> branchMap = b.getMap();
        if (!reset) {
            if (!branchMap.containsKey(branchname[1])) {
                System.out.print("No such branch exists.");
                System.exit(0);
            } else if (b.getCurrent().equals(branchname[1])) {
                System.out.print("No need to checkout the current branch.");
                System.exit(0);
            }
        } else {
            b.reset();
        }
        /* if a working file is untracked in the current branch, error. */
        File f = new File(".gitlet/commits/" + b.getcommit(branchname[1]));
        Set<String> filenames = Utils.readObject(f, MyCommit.class).getBlobs().keySet();
        Set<String> currfilenames = currcommit().getBlobs().keySet();
        /* the name list of all files in the working directory */
        List<String> workingfiles = Utils.plainFilenamesIn(User.workingdir);
        /* if a working file is untracked in the current branch, error. */
        for (String s : workingfiles) {
            if (!currfilenames.contains(s) && filenames.contains(s)) {
                System.out.print("There is an untracked file in the way; "
                        + "delete it or add it first.");
                System.exit(0);
            }
        }
        File[] files = workingdir.listFiles();
        /* Overwrite files in the Working_dir */
        for (File f1 : files) {
            if (!filenames.contains(f1.getName())) {
                Utils.restrictedDelete(f1);
            }
        }
        for (String s : filenames) {
            File in = new File(".gitlet/blobs/"
                    + Utils.readObject(f, MyCommit.class).getBlobs().get(s));
            byte[] filecontent = Utils.readObject(in, Blob.class).getContents();
            Utils.writeContents(new File("./" + s), filecontent);
        }
        /* clear the staging area */
        File[] files2 = sarea.listFiles();
        if (files2 != null) {
            for (File f2 : files2) {
                //Utils.restrictedDelete(f2);
                f2.delete();
            }
        }
        b.change(branchname[1]);
        Utils.writeObject(branches, b);
    }

    public void rmbranch(String[] branchname) {
        if (branchname.length != 2) {
            throw new IllegalArgumentException("Incorrect operands.");
        }
        MyBranch b = Utils.readObject(new File(".gitlet/branches"), MyBranch.class);
        if (!b.getMap().keySet().contains(branchname[1])) {
            System.out.print("A branch with that name does not exists.");
            System.exit(0);
        } else if (b.getCurrent().equals(branchname[1])) {
            System.out.print("Cannot remove the current branch.");
            System.exit(0);
        }
        b.removebranch(branchname[1]);
        Utils.writeObject(new File(".gitlet/branches"), b);
    }

    public void find(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Incorrect operands.");
        }
        boolean found = false;
        String message = args[1];
        List<String> commits =
                Utils.plainFilenamesIn(new File(".gitlet/commits"));
        if (commits != null) {
            for (String name : commits) {
                MyCommit commitq =
                        Utils.readObject(new File(".gitlet/commits/" + name), MyCommit.class);
                if (commitq.getLog().equals(message)) {
                    found = true;
                    System.out.println(name);
                }
            }
        }
        if (!found) {
            throw new RuntimeException("Found no commit with that message.");
        }
    }

    public void rm(String[] filename) {
        if (filename.length != 2) {
            throw new IllegalArgumentException("Incorrect operands.");
        }
        String file = filename[1];
        List<String> filenames = Utils.plainFilenamesIn(sarea);
        boolean remove1 = false;
        boolean remove2 = false;
        if (filenames != null && filenames.contains(file)) {
            remove1 = true;
        }
        if (currcommit().getBlobs().keySet().contains(file)) {
            remove2 = true;
        }

        if (remove1 || remove2) {
            if (remove1) {
                new File(".gitlet/staging_area/" + file).delete();
            }
            if (remove2) {
                File removef = new File(".gitlet/removed");
                Removed removedfile = Utils.readObject(removef, Removed.class);
                removedfile.add(file);
                Utils.writeObject(new File(".gitlet/removed"), removedfile);
                Utils.restrictedDelete(new File("./" + file));
            }
        } else {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    public void log() {
        String commitname = currbranch().getCurrentcommit();

        while (commitname != null) {
            MyCommit commitq =
                    Utils.readObject(new File(".gitlet/commits/" + commitname), MyCommit.class);
            Formatter printer = new Formatter();
            printer.format("===%n");
            printer.format("Commit %s%n", commitname);

            if (commitq.merged()) {
                String mergeParent = commitq.getmergeparent().substring(0, 7);
                String parent = commitq.getParent().substring(0, 7);
                printer.format("Merge: %s %s%n", parent, mergeParent);
            }

            Date time = commitq.getDateTime();
            SimpleDateFormat dateFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            printer.format("%s%n%s%n", dateFormate.format(time), commitq.getLog());
            System.out.println(printer);

            commitname = commitq.getParent();
        }
    }

    public void globallog() {
        List<String> commits = Utils.plainFilenamesIn(commit);
        for (String commitq : commits) {
            Formatter printer = new Formatter();
            MyCommit com =
                    Utils.readObject(new File(".gitlet/commits/" + commitq), MyCommit.class);
            printer.format("===%n");
            Date time = com.getDateTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            printer.format("Commit %s%n", commitq, dateFormat.format(time), com.getLog());
            if (com.merged()) {
                String mergeParent = com.getmergeparent().substring(0, 7);
                String parent = com.getParent().substring(0, 7);
                printer.format("Merge: %s %s%n", parent, mergeParent);
            }
            printer.format("%s%n%s%n", dateFormat.format(time), com.getLog());
            System.out.println(printer);
        }
    }

    public void reset(String[] commitid) {
        /* no id with the given commit exists */
        if (commitid.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        List<String> commitnames = Utils.plainFilenamesIn(commit);
        if (!commitnames.contains(commitid[1])) {
            System.out.print("No commit with that id exists.");
            System.exit(0);
        }
        List<String> workingfiles = Utils.plainFilenamesIn(User.workingdir);
        List<String> stagedfiles = Utils.plainFilenamesIn(User.sarea);
        /* if a working file is untracked in the current branch, error. */
        for (String s : workingfiles) {
            if (!currcommit().getBlobs().keySet().contains(s)
                    && !stagedfiles.contains(s)) {
                System.out.print("There is an untracked file in the way; "
                        + "delete it or add it first.");
                System.exit(0);
            }
        }
        MyBranch b = Utils.readObject(branches, MyBranch.class);
        String currb = b.getCurrent();
        String[] args = new String[]{"checkout", currb};
        b.setReset(commitid[1]);
        Utils.writeObject(branches, b);
        checkoutBranch(args, true);
    }

    public void status() {
        Formatter printer = new Formatter();

        /* Read and print all the branches*/
        MyBranch branch = currbranch();
        String currentbranch = branch.getCurrent();
        printer.format("=== Branches ===%n");
        printer.format("*%s%n", currentbranch);
        for (String b : branch.getMap().keySet()) {
            if (!b.equals(currentbranch)) {
                printer.format("%s%n", b);
            }
        }
        /* Read and print all the files in the staging area*/
        printer.format("%n=== Staged Files ===%n");
        List<String> stagedFiles = Utils.plainFilenamesIn(sarea);
        if (stagedFiles != null) {
            for (String filename : stagedFiles) {
                printer.format("%s%n", filename);
            }
        }
        /* Read and print files that are removed*/
        printer.format("%n=== Removed Files ===%n");
        File r = new File(".gitlet/removed");
        Removed removedf = Utils.readObject(r, Removed.class);
        if (removedf.get() != null) {
            for (String name : removedf.get()) {
                printer.format("%s%n", name);
            }
        }

        /* Print modifications noe staged for commit*/
        printer.format("%n=== Modifications Not Staged For Commit ===%n");

        /* Print untracked files*/
        printer.format("%n=== Untracked Files ===%n");

        System.out.println(printer);
    }

    /** Merges files from the given branch into the current branch. */
    /** ARGS.*/
    public void merge(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        MyBranch branch = Utils.readObject(branches, MyBranch.class);
        if (!branch.getMap().containsKey(args[1])) {
            System.out.println(" A branch with that name does not exist.");
            System.exit(0);
        } else if (args[1].equals(branch.getCurrent())) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        merge(args[1]);
    }

    private HashMap<String, String> currblobs;
    private MyCommit check;
    private String splitname = null;
    private MyBranch checkbranch;
    private String givencommitname;
    private Set<String> givenfilenames;
    private String currentname;
    private Set<String> currentfilenames;
    private HashMap<String, String> splitblos;
    private HashMap<String, String> givenblobs;
    private ArrayList<String> all = new ArrayList<>();
    /*concept inspired @benny*/
    private void merge(String givenname) {
        checkbranch = Utils.readObject(branches, MyBranch.class);
        String currentbranch = checkbranch.getCurrent();
        currentname = checkbranch.getCurrentcommit();
        givencommitname = checkbranch.getcommit(givenname);
        File commitfile = new File(".gitlet/commits/" + currentname);
        MyCommit currentcommit = Utils.readObject(commitfile, MyCommit.class);
        File givenfile = new File(".gitlet/commits/" + givencommitname);
        MyCommit givencommit = Utils.readObject(givenfile, MyCommit.class);
        check = currentcommit;
        givenblobs = givencommit.getBlobs();
        givenfilenames = givenblobs.keySet();
        currblobs = currentcommit.getBlobs();
        currentfilenames = currblobs.keySet();
        List<String> stagedfiles = Utils.plainFilenamesIn(sarea);
        if (stagedfiles != null && stagedfiles.size() != 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        File removed = new File(".gitlet/removed");
        Removed remove = Utils.readObject(removed, Removed.class);
        if (remove.get().size() > 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (failure3()) {
            //System.out.println("3");
            return;
        }
        check = givencommit;
        if (failure1()) {
            //System.out.println("1");
            return;
        }
        if (failure2()) {
            //System.out.println("2");
            return;
        }
        File splitfile = new File(".gitlet/commits/" + splitname);
        MyCommit splitcommit = Utils.readObject(splitfile, MyCommit.class);
        splitblos = splitcommit.getBlobs();
        boolean conflict1 = false;
        conflict1 = checkgiven();
        boolean conflict2 = checkcurrent();
        if (conflict1 || conflict2) {
            System.out.println("Encountered a merge conflict.");
            System.exit(0);
        }

        MyCommit tmp = givencommit;
        while (tmp.getParent() != null) {
            String parentName = tmp.getParent();
            if (parentName.equals(currentname)) {
                checkbranch.change(givencommitname);
                System.out.println("Current branch fast-forwarded.");
                return;
            }
            File parentFile = new File(".gitlet/commits/" + parentName);
            tmp = Utils.readObject(parentFile, MyCommit.class);
        }

        String[] args =
                new String[]{"commit", "Merged " + currentbranch + " with " + givenname + "."};
        commit(args);
    }

    /** failure case 1*/
    private boolean failure1() {
        while (check.getParent() != null) {
            String checkparent = check.getParent();
            if (all.contains(checkparent)) {
                if (checkparent.equals(currentname)) {
                    checkbranch.change(givencommitname);
                    System.out.println("Current branch fast-forwarded.");
                    return true;
                } else {
                    splitname = checkparent;
                }
                break;
            }
            File parentfile = new File(".gitlet/commits/" + checkparent);
            check = Utils.readObject(parentfile, MyCommit.class);
        }
        return false;
    }


    /** failure case 2.*/
    private boolean failure2() {
        File[] workingfiles = workingdir.listFiles();
        if (workingfiles != null) {
            for (File each : workingfiles) {
                if (!each.isDirectory()) {
                    String name = each.getName();
                    if (!currentfilenames.contains(name)) {
                        if (givenfilenames.contains(name)) {
                            System.out.println("There is an untracked file in the way;"
                                    + "delete it or add it first.");
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /** */
    private boolean failure3() {
        String tmp = currentname;
        while (check.getParent() != null) {
            if (tmp.equals(givencommitname)) {
                System.out.println("Given branch is "
                        + "an ancestor of the current branch.");
                return true;
            }
            all.add(tmp);
            String parent = check.getParent();
            File parentfile = new File(".gitlet/commits/" + parent);
            check = Utils.readObject(parentfile, MyCommit.class);
            tmp = parent;
        }
        if (tmp.equals(givencommitname)) {
            System.out.println("Given branch is "
                    + "an ancestor of the current branch.");
            return true;
        }
        return false;
    }

    private boolean checkgiven() {
        boolean conflict = false;
        for (String filename : givenfilenames) {
            String givenblobcont = givenblobs.get(filename);
            String splitblobcont = splitblos.get(filename);
            String currentblobcont = currblobs.get(filename);
            if (!givenblobcont.equals(currentblobcont)) {
                if (!givenblobcont.equals(splitblobcont)) {
                    File blobfile = new File(".gitlet/blobs/" + givenblobcont);
                    Blob blob = Utils.readObject(blobfile, Blob.class);
                    byte[] filecontent = blob.getContents();
                    if ((currentblobcont != null
                            && currentblobcont.equals(splitblobcont))
                            || (currentblobcont == null
                            && splitblobcont == null)) {
                        File stagings = new File(".gitlet/staging_area/" + filename);
                        Utils.writeContents(stagings, filecontent);
                        File checkouting = new File("./" + filename);
                        Utils.writeContents(checkouting, filecontent);

                    } else {
                        File mergefile = new File("./" + filename);
                        conflict = true;
                        if (currblobs.containsKey(filename)) {
                            File currentblobfile = new File(".gitlet/blobs/"
                                    + currentblobcont);
                            Blob currentblob = Utils.readObject(currentblobfile,
                                    Blob.class);
                            byte[] currentcontent = currentblob.getContents();
                            Utils.writeContents(mergefile, "<<<<<<< HEAD\n",
                                    currentcontent, "=======\n",
                                    filecontent, ">>>>>>>\n");
                        } else {
                            Utils.writeContents(mergefile, "<<<<<<< HEAD\n",
                                    "=======\n", filecontent, ">>>>>>>\n");
                        }
                    }
                }
            }
        }
        return conflict;
    }

    private boolean checkcurrent() {
        boolean conflict = false;
        for (String filename : currentfilenames) {
            String splitblobcont = splitblos.get(filename);
            if (!givenblobs.containsKey(filename)) {
                if (currblobs.get(filename).equals(splitblobcont)) {
                    currblobs.remove(filename);
                    File working = new File("./" + filename);
                    if (working.exists()) {
                        Utils.restrictedDelete(working);
                    }
                } else if (splitblobcont != null) {
                    conflict = true;
                    File mergefile = new File("./" + filename);
                    String currentblobcont = currblobs.get(filename);
                    File currentblobfile = new File(".gitlet/blobs/"
                            + currentblobcont);
                    Blob currentblob = Utils.readObject(currentblobfile,
                            Blob.class);
                    byte[] currentcontent = currentblob.getContents();
                    Utils.writeContents(mergefile,
                            "<<<<<<< HEAD\n", currentcontent,
                            "=======\n", ">>>>>>>\n");
                }
            }
        }
        return conflict;
    }
}
