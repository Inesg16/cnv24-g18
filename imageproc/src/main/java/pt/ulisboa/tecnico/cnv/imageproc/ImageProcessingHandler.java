package pt.ulisboa.tecnico.cnv.imageproc;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import pt.ulisboa.tecnico.cnv.javassist.tools.ICount;

public abstract class ImageProcessingHandler implements HttpHandler, RequestHandler<Map<String,String>, String> {

    abstract BufferedImage process(BufferedImage bi) throws IOException;

    private String handleRequest(String inputEncoded, String format) {
        byte[] decoded = Base64.getDecoder().decode(inputEncoded);
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
            BufferedImage bi = ImageIO.read(bais);
            bi = process(bi);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, format, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return e.toString();
        }
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        // Handling CORS
        t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            t.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            t.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            t.sendResponseHeaders(204, -1);
            return;
        }

        InputStream stream = t.getRequestBody();
        // Result syntax: data:image/<format>;base64,<encoded image>
        String result = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
        String[] resultSplits = result.split(",");
        String format = resultSplits[0].split("/")[1].split(";")[0];

        String output = handleRequest(resultSplits[1], format);
        output = String.format("data:image/%s;base64,%s", format, output);

        t.sendResponseHeaders(200, output.length());
        OutputStream os = t.getResponseBody();
        os.write(output.getBytes());
        os.close();

        // After completing the request, retrieve metrics from ICount
        long methodsExecuted = ICount.getExecutedMethodCount();
        long basicBlocksExecuted = ICount.getExecutedBasicBlockCount();
        long instructionsExecuted = ICount.getExecutedInstructionCount();

        // Log metrics (you can also save them to a file here)
        System.out.println("Metrics after request:");
        System.out.println("Methods Executed: " + methodsExecuted);
        System.out.println("Basic Blocks Executed: " + basicBlocksExecuted);
        System.out.println("Instructions Executed: " + instructionsExecuted);
    }

    @Override
    public String handleRequest(Map<String,String> event, Context context) {
        return handleRequest(event.get("body"), event.get("fileFormat"));
    }
}
