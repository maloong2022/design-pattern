import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
    private FileWriter writer;
    private static final Logger instance = new Logger();



    private Logger(){
        File file = new File("./log.txt");
        try {
			writer = new FileWriter(file,true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}// true: set the file open with append mode.
    }

    public static Logger getInstance(){
        return instance;
    }

    public void log(String message){
        try {
			writer.write(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}