package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("icount-metrics.out", true))) {
            writer.write(String.format("--- \n"));
            writer.write(String.format("%s [%s] Number of executed methods: %s \n", currentThreadId, ICount.class.getSimpleName(), nmethods));
            writer.write(String.format("%s [%s] Number of executed basic blocks: %s \n", currentThreadId, ICount.class.getSimpleName(), nblocks));
            writer.write(String.format("%s [%s] Number of executed instructions: %s \n", currentThreadId, ICount.class.getSimpleName(), ninsts));
            writer.write(String.format("--- \n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(String.format("[%s] Number of executed methods: %s", ICount.class.getSimpleName(), nmethods));
        System.out.println(String.format("[%s] Number of executed basic blocks: %s", ICount.class.getSimpleName(), nblocks));
        System.out.println(String.format("[%s] Number of executed instructions: %s", ICount.class.getSimpleName(), ninsts));
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
