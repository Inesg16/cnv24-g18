package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javassist.CannotCompileException;
import javassist.CtBehavior;


public class ICount extends CodeDumper {

    /**
     * Number of executed basic blocks.
     */
    private static long nblocks = 0;

    /**
     * Number of executed methods.
     */
    private static long nmethods = 0;

    /**
     * Number of executed instructions.
     */
    private static long ninsts = 0;

    public ICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static long getExecutedMethodCount() {
        return nmethods;
    }

    public static long getExecutedBasicBlockCount() {
        return nblocks;
    }

    public static long getExecutedInstructionCount() {
        return ninsts;
    }

    public static void incBasicBlock(int position, int length) {
        nblocks++;
        ninsts += length;
    }

    public static void incBehavior(String name) {
        nmethods++;
    }

    public static void printStatistics() throws IOException {

        long currentThreadId = Thread.currentThread().getId();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(dtf);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("icount-metrics.out", true))) {
            /*
             * [Thread ID] Time @request-type | Num-executed-methods | Num-executed-bb | Num-executed-instructions
             */
            writer.write(String.format("[%s] %s @unknown | %s | %s | %s\n", currentThreadId, time, nmethods, nblocks, ninsts));
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
         * [Thread ID] Time @request-type | Num-executed-methods | Num-executed-bb | Num-executed-instructions
         */
        System.out.println(String.format("[%s] %s @unknown | %s | %s | %s\n", currentThreadId, time, nmethods, nblocks, ninsts));
    }

    public static void printStatistics(String reqtype) throws IOException {

        long currentThreadId = Thread.currentThread().getId();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(dtf);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("icount-metrics.out", true))) {
            /*
             * [Thread ID] Time @request-type | Num-executed-methods | Num-executed-bb | Num-executed-instructions
             */
            writer.write(String.format("[%s] %s @%s | %s | %s | %s\n", currentThreadId, time, reqtype, nmethods, nblocks, ninsts));
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
         * [Thread ID] Time @request-type | Num-executed-methods | Num-executed-bb | Num-executed-instructions
         */
        System.out.println(String.format("[%s] %s @%s | %s | %s | %s\n", currentThreadId, time, reqtype, nmethods, nblocks, ninsts));
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", ICount.class.getName(), behavior.getLongName()));

        if (behavior.getName().equals("main")) {
            behavior.insertAfter(String.format("%s.printStatistics();", ICount.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", ICount.class.getName(), block.getPosition(), block.getLength()));
    }

}
