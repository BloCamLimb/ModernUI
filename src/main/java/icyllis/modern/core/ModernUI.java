package icyllis.modern.core;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

@Mod(ModernUI.MODID)
public class ModernUI {

    public static final String MODID = "modernui";

    public static Logger logger = LogManager.getLogger("ModernUI");
    public static Marker marker = MarkerManager.getMarker("Main");

    public ModernUI() {

    }

}
