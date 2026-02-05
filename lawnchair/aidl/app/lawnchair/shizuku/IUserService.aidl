package app.lawnchair.shizuku;

interface IUserService {
    void runCommand(in String[] command, in String[] env);
    void destroy();
}
