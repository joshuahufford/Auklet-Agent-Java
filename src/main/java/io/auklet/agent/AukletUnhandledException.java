package io.auklet.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.Instant;

public class AukletUnhandledException implements Thread.UncaughtExceptionHandler {

    private Thread.UncaughtExceptionHandler defaultExceptionHandler;
    private List<Object> stackTrace;

    public AukletUnhandledException(Thread.UncaughtExceptionHandler defaultExceptionHandler) {
        this.defaultExceptionHandler = defaultExceptionHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable thrown) {

        if (defaultExceptionHandler != null) {
            // call the original handler
            defaultExceptionHandler.uncaughtException(thread, thrown);
            System.out.println("We are here");
        }

        else if (!(thrown instanceof ThreadDeath)) {
            List<Object> list = new ArrayList<>();
            //MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
            // CHECKSTYLE.OFF: RegexpSinglelineJava
            System.err.print("Exception in thread \"" + thread.getName() + "\" ");
            thrown.printStackTrace(System.err);

            System.out.println("library Uncaught Exception caught from app  " + thrown.getMessage());
            System.out.println("Uncaught Exception stacktrace is ");
            for (StackTraceElement se : thrown.getStackTrace()) {
                Map<String, Object> map = new HashMap<>();
                map.put("functionName", se.getMethodName());
                map.put("className", se.getClassName());
                map.put("filePath", se.getFileName());
                map.put("lineNumber", se.getLineNumber());
                list.add(map);
                System.out.println(list);
            }
            setStackTrace(list, thrown.getMessage());
            Messages.createMessagePack();
        }


    }

    public static AukletUnhandledException setup() {

        System.out.println("Configuring uncaught exception handler.");
        Thread.UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (currentHandler != null) {
            System.out.println("default UncaughtExceptionHandler class='" + currentHandler.getClass().getName() + "'");
        }

        AukletUnhandledException handler = new AukletUnhandledException(currentHandler);
        Thread.setDefaultUncaughtExceptionHandler(handler);
        return handler;
    }

    private void setStackTrace(List<Object> stackTrace, String exceptionMessage){
        this.stackTrace = stackTrace;
        Messages.map.put("stackTrace", stackTrace);
        Messages.map.put("timestamp", Instant.now().getEpochSecond());
        Messages.map.put("excType", exceptionMessage);

    }

    protected List<Object> getStacktrace(){
        return this.stackTrace;
    }

}
