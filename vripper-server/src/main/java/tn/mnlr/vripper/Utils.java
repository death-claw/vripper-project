package tn.mnlr.vripper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {

    public static String throwableToString(Throwable th) throws IOException {
        try (StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter)) {
            th.printStackTrace(printWriter);
            printWriter.flush();
            stringWriter.flush();
            return stringWriter.toString();
        }
    }
}
