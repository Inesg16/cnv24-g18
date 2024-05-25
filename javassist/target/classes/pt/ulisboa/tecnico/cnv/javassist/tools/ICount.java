package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javassist.CannotCompileException;
import javassist.CtBehavior;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;


public class ICount extends CodeDumper {

    /**
     * Number of executed basic blocks.
     */
    private static Dictionary<Long, Integer> nblocks = new Hashtable<>();

    /**
     * Number of executed methods.
     */
    private static Dictionary<Long, Integer> nmethods= new Hashtable<>();

    /**
     * Number of executed instructions.
     */
    private static Dictionary<Long, Integer> ninsts= new Hashtable<>();

    public ICount(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static long getExecutedMethodCount() {
        return nmethods.get(Thread.currentThread().getId());
    }

    public static long getExecutedBasicBlockCount() {
        return nblocks.get(Thread.currentThread().getId());
    }

    public static long getExecutedInstructionCount() {
        return ninsts.get(Thread.currentThread().getId());
    }

    public static void incBasicBlock(int position, int length) {
        if(nblocks.get(Thread.currentThread().getId()) != null){
            int updatedValue = nblocks.get(Thread.currentThread().getId()) + 1;
            nblocks.put(Thread.currentThread().getId(), updatedValue);
        }
        else{
            nblocks.put(Thread.currentThread().getId(), 1);
        }
        if(ninsts.get(Thread.currentThread().getId()) != null){
            int updatedValue = ninsts.get(Thread.currentThread().getId()) + 1;
            ninsts.put(Thread.currentThread().getId(), updatedValue);
        }
        else{
            ninsts.put(Thread.currentThread().getId(), 1);
        }
    }

    public static void incBehavior(String name) {
        if(nmethods.get(Thread.currentThread().getId()) != null){
            int updatedValue = nmethods.get(Thread.currentThread().getId()) + 1;
            nmethods.put(Thread.currentThread().getId(), updatedValue);
        }
        else{
            nmethods.put(Thread.currentThread().getId(), 1);
        }
    }

    public static void printStatistics() throws IOException {

        long currentThreadId = Thread.currentThread().getId();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(dtf);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("icount-metrics.out", true))) {
            /*
             * [Thread ID] Time @request-type | Num-executed-methods | Num-executed-bb | Num-executed-instructions
             */
            writer.write(String.format("[%s] %s @unknown | %s | %s | %s\n", currentThreadId, time, nmethods.get(Thread.currentThread().getId()), nblocks.get(Thread.currentThread().getId()), ninsts.get(Thread.currentThread().getId())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
         * [Thread ID] Time @request-type | Num-executed-methods | Num-executed-bb | Num-executed-instructions
         */
        System.out.println(String.format("[%s] %s @unknown | %s | %s | %s\n", currentThreadId, time, nmethods.get(Thread.currentThread().getId()), nblocks.get(Thread.currentThread().getId()), ninsts.get(Thread.currentThread().getId())));
        nblocks.remove(Thread.currentThread().getId());
        nmethods.remove(Thread.currentThread().getId());
        ninsts.remove(Thread.currentThread().getId());
    }

    public static void printStatistics(String reqtype) throws IOException {

        long currentThreadId = Thread.currentThread().getId();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(dtf);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("icount-metrics.out", true))) {
            /*
             * [Thread ID] Time @request-type | Num-executed-methods | Num-executed-bb | Num-executed-instructions
             */
            writer.write(String.format("[%s] %s @%s | %s | %s | %s\n", currentThreadId, time, reqtype, nmethods.get(Thread.currentThread().getId()), nblocks.get(Thread.currentThread().getId()), ninsts.get(Thread.currentThread().getId())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
         * [Thread ID] Time @request-type | Num-executed-methods | Num-executed-bb | Num-executed-instructions
         */
        System.out.println(String.format("[%s] %s @%s | %s | %s | %s\n", currentThreadId, time, reqtype, nmethods.get(Thread.currentThread().getId()), nblocks.get(Thread.currentThread().getId()), ninsts.get(Thread.currentThread().getId())));
        nblocks.remove(Thread.currentThread().getId());
        nmethods.remove(Thread.currentThread().getId());
        ninsts.remove(Thread.currentThread().getId());
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
