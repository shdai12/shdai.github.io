package gitlet;

import java.io.File;

public class Command {
    private User user = new User();

    /**Check if the directory is initiated*/
    private static void check() {
        File dir = new File(".gitlet");
        if (!dir.exists()) {
            System.out.println("Not in an initialized gitlet directory.");
            System.exit(0);
        }
    }

    /**Parse the command and search for the correct method to use*/
    public void searchcommand(String[] args) {
        try {
            if (args[0].equals("init")) {
                user.init();
            } else if (args[0].equals("add")) {
                check();
                user.add(args);
            } else if (args[0].equals("find")) {
                check();
                user.find(args);
            } else if (args[0].equals("commit")) {
                check();
                user.commit(args);
            } else if (args[0].equals("status")) {
                check();
                user.status();
            } else if (args[0].equals("log")) {
                check();
                user.log();
            } else if (args[0].equals("global-log")) {
                check();
                user.globallog();
            } else if (args[0].equals("branch")) {
                check();
                user.branch(args);
            } else if (args[0].equals("rm-branch")) {
                check();
                user.rmbranch(args);
            } else if (args[0].equals("reset")) {
                check();
                user.reset(args);
            } else if (args[0].equals("merge")) {
                check();
                user.merge(args);
            } else if (args[0].equals("rm")) {
                check();
                user.rm(args);
            } else if (args[0].equals("checkout")) {
                check();
                user.selector(args);
            }
        } catch (ExceptionInInitializerError e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }
}
