package gitlet;

/* Driver class for Gitlet, the tiny stupid version-control system.
   @author Sam
*/
public class Main {

    /* Usage: java gitlet.Main ARGS, where ARGS contains
       <COMMAND> <OPERAND> .... */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println("Please enter a command.");
                System.exit(0);
            }
            Command c = new Command();
            c.searchcommand(args);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        } catch (RuntimeException r) {
            System.out.println(r.getMessage());
            System.exit(0);
        }
    }

}
