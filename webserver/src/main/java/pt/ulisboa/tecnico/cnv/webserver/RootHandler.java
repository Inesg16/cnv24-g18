package pt.ulisboa.tecnico.cnv.webserver;

import java.io.IOException;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.URI;

import pt.ulisboa.tecnico.cnv.javassist.tools.ICount;

public class RootHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange he) throws IOException {
        // Handling CORS
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if (he.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            he.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            he.sendResponseHeaders(204, -1);
            return;
        }

        // parse request
        URI requestedUri = he.getRequestURI();
        String query = requestedUri.getRawQuery();
        System.out.println(query);
        System.out.println("query");

        // After completing the request, retrieve metrics from ICount
        long methodsExecuted = ICount.getExecutedMethodCount();
        long basicBlocksExecuted = ICount.getExecutedBasicBlockCount();
        long instructionsExecuted = ICount.getExecutedInstructionCount();

        // Log metrics (you can also save them to a file here)
        System.out.println("Metrics after request:");
        System.out.println("Methods Executed: " + methodsExecuted);
        System.out.println("Basic Blocks Executed: " + basicBlocksExecuted);
        System.out.println("Instructions Executed: " + instructionsExecuted);

        he.sendResponseHeaders(200, 0);
    }
}
