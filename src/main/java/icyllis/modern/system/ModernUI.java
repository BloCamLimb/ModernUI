package icyllis.modern.system;

import icyllis.modern.api.ModernUIApi;
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
        ModernUI.LOGGER.info(MARKER, "{} has been initialized", ModernUIApi.INSTANCE.getDeclaringClass().getSimpleName());
    }

}
