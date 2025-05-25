package com.depavlo.gitlaberfx.util;

import com.depavlo.gitlaberfx.controller.ProgressDialogController;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Utility class for running tasks in the background with progress indicators.
 */
public class TaskRunner {
    private static final Logger logger = LoggerFactory.getLogger(TaskRunner.class);
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * Runs a task in the background with a progress dialog.
     *
     * @param stage the parent stage
     * @param title the dialog title
     * @param message the message to display
     * @param task the task to run
     * @param <T> the task result type
     */
    public static <T> void runWithProgress(Stage stage, String title, String message, Task<T> task) {
        ProgressDialogController controller = DialogHelper.showProgressDialog(stage, task, title, message);
        if (controller != null) {
            executor.submit(task);
        }
    }

    /**
     * Creates and runs a task in the background with a progress dialog.
     *
     * @param stage the parent stage
     * @param title the dialog title
     * @param message the message to display
     * @param action the action to perform
     * @param onSuccess the callback to run on success
     * @param <T> the task result type
     */
    public static <T> void runWithProgress(
            Stage stage,
            String title,
            String message,
            TaskWithProgress<T> action,
            Consumer<T> onSuccess) {
        
        // Create a task that will execute the action
        ProgressTask<T> task = new ProgressTask<>(action);
        
        // Set up success callback
        task.setOnSucceeded(event -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });
        
        // Show the progress dialog
        ProgressDialogController controller = DialogHelper.showProgressDialog(stage, task, title, message);
        
        // Set up a thread to check for cancellation requests
        if (controller != null) {
            Thread cancellationChecker = new Thread(() -> {
                while (!task.isCancelled() && !task.isDone()) {
                    task.setCancelRequested(controller.isCancelRequested());
                    task.setForceStopRequested(controller.isForceStopRequested());
                    
                    if (task.isForceStopRequested()) {
                        task.cancel(true);
                        break;
                    }
                    
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            cancellationChecker.setDaemon(true);
            cancellationChecker.start();
            
            // Run the task
            executor.submit(task);
        }
    }
    
    /**
     * Shuts down the executor service.
     */
    public static void shutdown() {
        executor.shutdown();
    }
    
    /**
     * Interface for tasks that report progress.
     *
     * @param <T> the task result type
     */
    public interface TaskWithProgress<T> {
        /**
         * Executes the task.
         *
         * @param progressCallback the callback for reporting progress
         * @return the task result
         * @throws Exception if an error occurs
         */
        T call(ProgressCallback progressCallback) throws Exception;
    }
    
    /**
     * Interface for reporting progress.
     */
    public interface ProgressCallback {
        /**
         * Updates the progress value.
         *
         * @param progress the progress value (0.0 to 1.0)
         */
        void updateProgress(double progress);
        
        /**
         * Updates the progress message.
         *
         * @param message the progress message
         */
        void updateMessage(String message);
        
        /**
         * Checks if cancellation has been requested.
         *
         * @return true if cancellation has been requested
         */
        boolean isCancelRequested();
        
        /**
         * Checks if force stop has been requested.
         *
         * @return true if force stop has been requested
         */
        boolean isForceStopRequested();
    }
    
    /**
     * A Task implementation that supports progress reporting and cancellation.
     *
     * @param <T> the task result type
     */
    private static class ProgressTask<T> extends Task<T> {
        private final TaskWithProgress<T> action;
        private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
        private final AtomicBoolean forceStopRequested = new AtomicBoolean(false);
        
        public ProgressTask(TaskWithProgress<T> action) {
            this.action = action;
        }
        
        @Override
        protected T call() throws Exception {
            // Create a progress callback that updates the task's progress and message
            ProgressCallback progressCallback = new ProgressCallback() {
                @Override
                public void updateProgress(double progress) {
                    Platform.runLater(() -> ProgressTask.this.updateProgress(progress, 1.0));
                }

                @Override
                public void updateMessage(String message) {
                    Platform.runLater(() -> ProgressTask.this.updateMessage(message));
                }

                @Override
                public boolean isCancelRequested() {
                    return cancelRequested.get() || isCancelled();
                }

                @Override
                public boolean isForceStopRequested() {
                    return forceStopRequested.get() || isCancelled();
                }
            };
            
            // Run the action
            return action.call(progressCallback);
        }
        
        public void setCancelRequested(boolean cancelRequested) {
            this.cancelRequested.set(cancelRequested);
        }
        
        public void setForceStopRequested(boolean forceStopRequested) {
            this.forceStopRequested.set(forceStopRequested);
        }
        
        public boolean isForceStopRequested() {
            return forceStopRequested.get();
        }
    }
}