package app.lawnchair.shizuku;

import android.os.Bundle;

interface IUserService {
    /** Runs a command as the shell user and returns its exit code, or -1 if it couldn't run. */
    int runCommand(in String[] command, in String[] env);
    void destroy();

    /** Recent tasks for a user, newest first: int[] "taskIds", String[] "components". */
    Bundle getRecentTasks(int maxTasks, int userId);
    /** The task's last screenshot as a HardwareBuffer under "buffer", or an empty bundle. */
    Bundle getTaskThumbnail(int taskId);
    boolean startTaskFromRecents(int taskId, in Bundle options);
    boolean removeTask(int taskId);
}
