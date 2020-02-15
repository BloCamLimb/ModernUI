package icyllis.modern.system;

import icyllis.modern.ui.blur.BlurHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

@Mod(ModernUI.MODID)
public class ModernUI {

    public static final String MODID = "modernui";

    public static final Logger LOGGER = LogManager.getLogger("ModernUI");
    public static final Marker MARKER = MarkerManager.getMarker("MAIN");

    public ModernUI() {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ModernUI.LOGGER.info(MARKER, "{} has been initialized", BlurHandler.INSTANCE.getDeclaringClass().getSimpleName()));
    }

}
