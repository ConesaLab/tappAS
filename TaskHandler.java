/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class TaskHandler extends AppObject {
    private final ArrayList<TaskInfo> lstTasks = new ArrayList<>();
    private ExecutorService executor = null;
    public ExecutorService getExecutor() { return executor; }

    public TaskHandler(App app) {
        super(app);
    }
    public void initialize(int threads) {
        executor = Executors.newFixedThreadPool(threads, (Runnable r) -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        });                
    }
    public void exit() {
        if(executor != null) {
            for(TaskInfo ti : lstTasks)
                ti.abort();
            executor.shutdownNow();
        }
    }
    public void addTask(TaskInfo taskInfo) { lstTasks.add(taskInfo); }
    public void removeTask(TaskInfo taskInfo) { lstTasks.remove(taskInfo); }
    public int getTasksCount() { return lstTasks.size(); }
    
    //
    // Data Classes
    //
    public static class ServiceExt extends Service<Void> {
        protected TaskInfo taskInfo = null;
        protected boolean pageDataReady = false;
        protected boolean taskCompleted = false;
        protected boolean taskAborted = false;
        protected boolean taskFailCancel = false;
        public boolean isTaskCompleted() { return taskCompleted; }
        public boolean pageDataReady() { return pageDataReady; }
        public void abort() {
            if(taskInfo != null) {
                taskAborted = true;
                taskInfo.abort();
            }
        }
        
        // do FX thread initialization
        public void initialize() {
            // override
        }
        protected void onRunning() {
            // override
        }
        protected void onStopped() {
            // override
        }
        protected void onFailed() {
            // override
        }
        protected void onCancelled() {
            // override
        }
        protected void onSucceeded() {
            // override
        }

        // do background thread work
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    return null;
                }
            };
        }

        // DO NOT override these functions
        @Override
        protected void running() {
            if(taskInfo != null)
                Tappas.getApp().getTaskHandler().addTask(taskInfo);
            onRunning();
        }
        @Override
        protected void succeeded() {
            taskCompleted = true;
            Tappas.getApp().getTaskHandler().removeTask(taskInfo);
            onSucceeded();
            onStopped();
        }
        @Override
        protected void failed() {
            taskCompleted = true;
            taskFailCancel = true;
            Tappas.getApp().getTaskHandler().removeTask(taskInfo);
            onFailed();
            onStopped();
        }
        @Override
        protected void cancelled() {
            taskCompleted = true;
            taskFailCancel = true;
            Tappas.getApp().getTaskHandler().removeTask(taskInfo);
            onCancelled();
            onStopped();
        }        
    }
    public static class TaskInfo {
        String name;
        Task<Void> task;
        Process process;
        boolean processAborted = false;
        
        public TaskInfo(String name, Task task) {
            this.name = name;
            this.task = task;
            process = null;
        }
        public boolean canAbort() {
            boolean result = false;
            if(process != null && process.isAlive())
                result = true;
            return result;
        }
        public void abort() {
            if(canAbort()) {
                processAborted = true;
                Tappas.getApp().logInfo("Killing process '" + process.toString() + "' for task '" + name + "'");
                try {
                    process.destroyForcibly();
                }
                catch (Exception e) {
                    System.out.println("Code exception destrying process by force within an exception.");
                }
            }
        }
    }
    
    // used for aborting task in cases where there are no external processes
    // the called function(s) will check abort flag and return accordingly
    public static class TaskAbortFlag {
        boolean abortFlag;
        public TaskAbortFlag(boolean flg) {
            abortFlag = flg;
        }
    }
}
