import java.io.IOException;
import java.util.*;
import java.util.logging.*;
public class ProcessLogger{
    static Logger logger;

    public static void logNotWorking(int pId) {
        System.out.println("log not found for " +pId);
    }

    public static Logger Loggermethod(Integer pId) throws IOException,SecurityException {
        Logger	loggerobj;
        loggerobj = Logger.getLogger(ProcessLogger.class.getName());
        loggerobj.log(Level.INFO,ProcessLogger.class.getCanonicalName());

        FileHandler fHandler = new FileHandler("logged_peerID - " + pId + ".log");
        SimpleFormatter formter = new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";    //format to print logs into log files for each peer

            @Override
            public synchronized String format(LogRecord peerlogRecords) {
                return String.format(format, new Date(peerlogRecords.getMillis()), peerlogRecords.getLevel().getLocalizedName(), peerlogRecords.getMessage());
            }
        };
        fHandler.setFormatter(formter);
        loggerobj.addHandler(fHandler);
        return loggerobj;
    }

}






