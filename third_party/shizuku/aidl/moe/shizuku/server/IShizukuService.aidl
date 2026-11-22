// 取自 RikkaApps/Shizuku-API（Apache-2.0）的裁剪版：
// 只声明 checkPermission(4) 与 newProcess(7)，事务码与上游保持一致。
package moe.shizuku.server;

import moe.shizuku.server.IRemoteProcess;

interface IShizukuService {
    int checkPermission(String permission) = 4;
    IRemoteProcess newProcess(in String[] cmd, in String[] env, in String dir) = 7;
}
