/*
 * This Classes are slightly modified Versions of the
 * AC.lib-ICO.
 * 
 * The original can be found here:
 * 
 * http://www.acproductions.de/commercial/aclibico/index.html
 * 
 * The code is based on:
 * 
 * winicontoppm.c - read a MS Windows .ico file and write portable pixmap(s)
 * 
 * Copyright (C) 2000 by Lee Benfield - lee@recoil.org
 * 
 * Permission to use, copy, modify, and distribute this software and its documentation for any purpose and without fee is hereby granted, provided that the above copyright notice appear in all copies and that both that copyright notice and this permission notice appear in supporting documentation. This software is provided "as is" without express or implied warranty.
 * 
 * Addendum for Java code by Christian Treber:
 * The rules for the Java adaption are the same as stated above for the C version.
 * 
 */
package webplugin.aclib.image.ico;

import java.awt.image.BufferedImage;
import java.io.IOException;

import webplugin.aclib.codec.AbstractDecoder;

/**
 * <p>
 * ARGB bitmap with 8 bits per color (32 bits per sample).
 * </p>
 * @author &copy; Christian Treber, ct@ctreber.com
 */
public class BitmapRGB32BPP extends AbstractBitmapRGB {
    /**
     * @param pDescriptor The image descriptor.
     */
    public BitmapRGB32BPP(final BitmapDescriptor pDescriptor) {
        super(pDescriptor);
    }

    /**
     * According to Microsoft, the topmost byte simply is not used, but I found
     * the fourth byte seems to be the alpha channel.
     * @param pDec
     *            The decoder.
     * @throws IOException
     */
    void read(final AbstractDecoder pDec) throws IOException {
        for (int lRowNo = 0; lRowNo < getHeight(); lRowNo++) {
            final byte[] lRow = pDec.readBytes(getWidth() * 4, null);
            int lRowByte = 0;
            int lOutputPos = (getHeight() - lRowNo - 1) * getWidth();
            for (int lColNo = 0; lColNo < getWidth(); lColNo++) {
                // BGRA -> ARGB, 8 bits per component.
                _samples[lOutputPos++] = lRow[lRowByte++]
                        + (lRow[lRowByte++] << 8) + (lRow[lRowByte++] << 16)
                        + (lRow[lRowByte++] << 24);
            }
        }
    }

    /**
     * @return Create an ARGB image.
     */
    public BufferedImage createImageRGB() {
        final BufferedImage lImage = new BufferedImage(getWidth(), getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        lImage.setRGB(0, 0, getWidth(), getHeight(), _samples, 0, getWidth());

        return lImage;
    }
}
