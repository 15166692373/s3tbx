/*
 *
 * Copyright (C) 2013-2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 * Copyright (C) 2014-2015 CS SI
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s2tbx.dataio.jp2;

import org.apache.commons.lang.SystemUtils;
import org.esa.s2tbx.dataio.Utils;
import org.esa.s2tbx.dataio.jp2.segments.CodingStyleDefaultSegment;
import org.esa.s2tbx.dataio.jp2.segments.ImageAndTileSizeSegment;
import org.esa.s2tbx.dataio.openjpeg.CommandOutput;
import org.esa.s2tbx.dataio.openjpeg.OpenJpegUtils;

import javax.imageio.stream.FileImageInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Created by opicas-p on 03/07/2014.
 */
public class CodeStreamUtils {

    public static ImageAndTileSizeSegment getSizeInfo(String uri, BoxReader.Listener listener) throws URISyntaxException, IOException {
        final File file = new File(CodeStreamUtils.class.getResource(uri).toURI());
        final FileImageInputStream stream = new FileImageInputStream(file);
        BoxReader boxReader = new BoxReader(stream, file.length(), listener);

        Box box;
        do {
            box = boxReader.readBox();
            if (box == null) {
                //todo change error messages
                throw new IllegalArgumentException("Wrong jpeg2000 format ?");
            }
        } while (!box.getSymbol().equals("jp2c"));

        boxReader.getStream().seek(box.getPosition() + box.getDataOffset());
        final CodestreamReader reader = new CodestreamReader(boxReader.getStream(),
                                                             box.getPosition() + box.getDataOffset(),
                                                             box.getLength() - box.getDataOffset());
        final MarkerSegment seg1 = reader.readSegment();

        final MarkerSegment seg2 = reader.readSegment();

        final ImageAndTileSizeSegment imageAndTileSizeSegment = (ImageAndTileSizeSegment) seg2;
        return imageAndTileSizeSegment;
    }

    public static CodingStyleDefaultSegment getCodingInfo(String uri, BoxReader.Listener listener) throws URISyntaxException, IOException {
        final File file = new File(CodeStreamUtils.class.getResource(uri).toURI());
        final FileImageInputStream stream = new FileImageInputStream(file);
        BoxReader boxReader = new BoxReader(stream, file.length(), listener);

        Box box;
        do {
            box = boxReader.readBox();
            if (box == null) {
                //todo change error messages
                throw new IllegalArgumentException("Wrong jpeg2000 format ?");
            }
        } while (!box.getSymbol().equals("jp2c"));

        boxReader.getStream().seek(box.getPosition() + box.getDataOffset());
        final CodestreamReader reader = new CodestreamReader(boxReader.getStream(),
                                                             box.getPosition() + box.getDataOffset(),
                                                             box.getLength() - box.getDataOffset());
        final MarkerSegment seg1 = reader.readSegment();
        final MarkerSegment seg2 = reader.readSegment();
        final MarkerSegment seg3 = reader.readSegment();

        CodingStyleDefaultSegment roar = (CodingStyleDefaultSegment) seg3;
        return roar;
    }

    public static TileLayout getTileLayout(URI uri, BoxReader.Listener listener) throws IOException {
        final File file = new File(uri);
        final FileImageInputStream stream = new FileImageInputStream(file);
        BoxReader boxReader = new BoxReader(stream, file.length(), listener);

        Box box;
        do {
            box = boxReader.readBox();
            if (box == null) {
                //todo change error messages
                throw new IllegalArgumentException("Wrong jpeg2000 format ?");
            }
        } while (!box.getSymbol().equals("jp2c"));

        boxReader.getStream().seek(box.getPosition() + box.getDataOffset());
        final CodestreamReader reader = new CodestreamReader(boxReader.getStream(),
                                                             box.getPosition() + box.getDataOffset(),
                                                             box.getLength() - box.getDataOffset());
        final MarkerSegment seg1 = reader.readSegment();
        final MarkerSegment seg2 = reader.readSegment();

        final ImageAndTileSizeSegment is = (ImageAndTileSizeSegment) seg2;
        MarkerSegment seg3 = null;

        do {
            seg3 = reader.readSegment();
        }
        while (!(seg3 instanceof CodingStyleDefaultSegment));

        CodingStyleDefaultSegment roar = (CodingStyleDefaultSegment) seg3;

        return new TileLayout((int) is.getXsiz(), (int) is.getYsiz(), (int) is.getXtsiz(), (int) is.getYtsiz(), getXNumTiles(is), getYNumTiles(is), roar.getLevels());
    }

    public static TileLayout getTileLayoutWithOpenJPEG(String opjdumpPath, URI uri) throws IOException, InterruptedException {
        Objects.requireNonNull(opjdumpPath);

        String pathToImageFile = uri.getPath().substring(1);
        if(SystemUtils.IS_OS_WINDOWS) {
            pathToImageFile = Utils.GetIterativeShortPathName(pathToImageFile);
        }

        String thePath = opjdumpPath;
        ProcessBuilder builder = new ProcessBuilder(thePath, "-i", pathToImageFile);
        builder.redirectErrorStream(true);

        CommandOutput exit = OpenJpegUtils.runProcess(builder);

        if (exit.getErrorCode() != 0) {
            StringBuffer sbu = new StringBuffer();
            for (String fragment : builder.command()) {
                sbu.append(fragment);
                sbu.append(' ');
            }

            throw new IOException(String.format("Command [%s] failed with error code [%d], stdoutput [%s] and stderror [%s]", sbu.toString(), exit.getErrorCode(), exit.getTextOutput(), exit.getErrorOutput()));
        }
        return OpenJpegUtils.parseOpjDump(exit.getTextOutput());
    }

    public static TileLayout getTileLayout(String opjdumpPath, URI imageFile, BoxReader.Listener listener, boolean nodump) throws IOException, InterruptedException {
        TileLayout myLayout = null;
        boolean decodingProblemSuspscted = false;

        try {
            myLayout = CodeStreamUtils.getTileLayout(imageFile, listener);
            if ((myLayout.numResolutions < 1) || (myLayout.numResolutions > 6)) {
                decodingProblemSuspscted = true;
            }
        } catch (IllegalArgumentException iae) {
            decodingProblemSuspscted = true;
        } catch (IOException e) {
            decodingProblemSuspscted = true;
        }

        if (decodingProblemSuspscted && !nodump) {
            myLayout = CodeStreamUtils.getTileLayoutWithOpenJPEG(opjdumpPath, imageFile);
        }

        return myLayout;
    }

    public static TileLayout getTileLayout(String uri, BoxReader.Listener listener) throws URISyntaxException, IOException {
        final URI partial = CodeStreamUtils.class.getResource(uri).toURI();
        return getTileLayout(partial, listener);
    }

    public static int getNumTiles(ImageAndTileSizeSegment imageAndTileSizeSegment) {
        double xTiles = Math.ceil((imageAndTileSizeSegment.getXsiz() - imageAndTileSizeSegment.getXosiz()) / (float) imageAndTileSizeSegment.getXtsiz());
        double yTiles = Math.ceil((imageAndTileSizeSegment.getYsiz() - imageAndTileSizeSegment.getYosiz()) / (float) imageAndTileSizeSegment.getYtsiz());

        int numTiles = (int) (xTiles * yTiles);

        return numTiles;
    }

    public static int getYNumTiles(ImageAndTileSizeSegment imageAndTileSizeSegment) {
        double yTiles = Math.ceil((imageAndTileSizeSegment.getYsiz() - imageAndTileSizeSegment.getYosiz()) / (float) imageAndTileSizeSegment.getYtsiz());

        return (int) yTiles;
    }

    public static int getXNumTiles(ImageAndTileSizeSegment imageAndTileSizeSegment) {
        double xTiles = Math.ceil((imageAndTileSizeSegment.getXsiz() - imageAndTileSizeSegment.getXosiz()) / (float) imageAndTileSizeSegment.getXtsiz());

        return (int) xTiles;
    }
}
