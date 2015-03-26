/*
 *
 *  * Copyright (C) 2015 CS SI
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package jp2.boxes;

import jp2.Box;
import jp2.BoxReader;
import jp2.BoxType;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * @author Norman Fomferra
 */
public class ColorSpecificationBox extends Box {

    private int meth;
    private int prec;
    private int approx;
    private long enumCS;
    private byte[] profile;

    public ColorSpecificationBox(BoxType type, long position, long length, int dataOffset) {
        super(type, position, length, dataOffset);
    }

    public int getMeth() {
        return meth;
    }

    public int getPrec() {
        return prec;
    }

    public int getApprox() {
        return approx;
    }

    public long getEnumCS() {
        return enumCS;
    }

    public byte[] getProfile() {
        return profile;
    }

    @Override
    public void readFrom(BoxReader reader) throws IOException {
        final ImageInputStream stream = reader.getStream();
        meth = (stream.readByte() & 0x000000ff);
        prec = stream.readByte();
        approx = (stream.readByte() & 0x000000ff);
        enumCS = (stream.readInt() & 0x00000000ffffffffL);
        final long profileLength = length - (stream.getStreamPosition() - position);
        profile = new byte[(int) profileLength];
        stream.read(profile);
    }
}
