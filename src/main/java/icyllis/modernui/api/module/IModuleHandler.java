package icyllis.modernui.api.module;

import icyllis.modernui.api.content.IFinish;
import icyllis.modernui.api.internal.IModuleReceiver;

public interface IModuleHandler extends IFinish {

    IModuleHandler track(IModuleReceiver receiver, IModernModule modernModule);

    IModuleHandler setMain();
}
