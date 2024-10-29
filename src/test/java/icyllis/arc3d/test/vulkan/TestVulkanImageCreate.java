/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test.vulkan;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.Image;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestVulkanImageCreate {

    public static final Logger LOGGER = LoggerFactory.getLogger("Arc3D");

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GLFW.glfwInit();
        LOGGER.info(Long.toString(ProcessHandle.current().pid()));
        try (var init = new TestVulkanInit(LOGGER)) {
            init.initialize();
            ContextOptions options = new ContextOptions();
            options.mLogger = LOGGER;
            @SharedPtr
            ImmediateContext context = init.createContext(options);
            if (context == null) {
                LOGGER.error("Failed to create Vulkan context");
                return;
            }
            ImageDesc imageDesc = context.getCaps().getDefaultColorImageDesc(
                    Engine.ImageType.k2D,
                    ColorInfo.CT_RGBA_8888,
                    1024, 1024, 1,
                    ISurface.FLAG_SAMPLED_IMAGE
            );
            if (imageDesc != null) {
                @SharedPtr
                Image image1 = context.getResourceProvider().findOrCreateImage(
                    imageDesc, true, "TestVulkanImage1"
                );
                LOGGER.info("Created image1: {}", image1);
                RefCnt.move(image1);
                @SharedPtr
                Image image2 = context.getResourceProvider().findOrCreateImage(
                        imageDesc, true, "TestVulkanImage2"
                );
                LOGGER.info("Created image2: {}", image2);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                RefCnt.move(image2);
            }
            context.unref();
        } finally {
            GLFW.glfwTerminate();
        }
    }
}
