// 取自 RikkaApps/Shizuku-API（Apache-2.0）的裁剪版。
// 事务码必须与上游一致，否则调用会错位。
package moe.shizuku.server;

interface IRemoteProcess {
    int waitFor() = 4;
    int exitValue() = 5;
    void destroy() = 6;
    boolean alive() = 7;
}
