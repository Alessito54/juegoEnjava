package org.drgnst.game;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ResourceLoader {

    /**
     * Load an image from filesystem or classpath. Path is project-relative like "image/bala.png".
     */
    public static BufferedImage loadImage(String path) throws IOException {
        // Try filesystem first
        File f = new File(path);
        if (f.exists()) {
            return ImageIO.read(f);
        }

        // Try classpath
        InputStream is = ResourceLoader.class.getResourceAsStream(path.startsWith("/") ? path : "/" + path);
        if (is == null) {
            // try without leading slash
            is = ResourceLoader.class.getResourceAsStream(path);
        }
        if (is == null) {
            throw new IOException("Resource not found: " + path);
        }

        BufferedInputStream bis = new BufferedInputStream(is);
        return ImageIO.read(bis);
    }
}
