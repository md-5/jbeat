import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("You must specify 3 arguments!");
            System.err.println("\tjava -jar jbeat.jar original.jar patch.bps output.jar")
            System.err.println("In This will apply patch.bps to original.jar and safe to output.jar");
            return;
        }
        File in = new File(args[0]);
        File patch = new File(args[1]);
        File out = new File(args[2]);
        
        if (out.exists()) {
            System.err.println("The output file " + args[2] + " exists.")
            System.err.println("Please delete it before running this program");
            return;
        }
        out.createNewFile()
        
        try {
            Patcher patcher = new Patcher(patch, in, out);
        } catch (Exception e) {
            System.err.println("Unable to patch file");
            e.printStackTrace();
            return;
        }
        
        System.out.prinln("Successfuly Patched: " args[0] " with " + args[1]);
    }
}
