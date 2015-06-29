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

package org.esa.s2tbx.dataio.s2.l1c;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.s2tbx.dataio.s2.S2SceneDescription;
import org.esa.snap.util.SystemUtils;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Norman Fomferra
 */
public class L1cSceneDescription extends S2SceneDescription {

    private final TileInfo[] tileInfos;
    private final Envelope2D sceneEnvelope;
    private final Rectangle sceneRectangle;
    private final Map<String, TileInfo> tileInfoMap;
    private final L1cMetadata.Tile.idGeom geometry;

    public L1cMetadata.Tile.idGeom getGeometry() {
        return geometry;
    }

    private static class TileInfo {
        private final int index;
        private final String id;
        private final Envelope2D envelope;
        private final Rectangle rectangle;

        public TileInfo(int index, String id, Envelope2D envelope, Rectangle rectangle) {
            this.index = index;
            this.id = id;
            this.envelope = envelope;
            this.rectangle = rectangle;
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    public static L1cSceneDescription create(L1cMetadata header, L1cMetadata.Tile.idGeom index, S2Config config) {
        List<L1cMetadata.Tile> tileList = header.getTileList();
        CoordinateReferenceSystem crs = null;
        Envelope2D[] tileEnvelopes = new Envelope2D[tileList.size()];
        TileInfo[] tileInfos = new TileInfo[tileList.size()];
        Envelope2D sceneEnvelope = null;

        if (tileList.isEmpty()) {
            throw new IllegalStateException();
        }
        for (int i = 0; i < tileList.size(); i++) {
            L1cMetadata.Tile tile = tileList.get(i);
            if (crs == null) {
                try {
                    crs = CRS.decode(tile.horizontalCsCode);
                    SystemUtils.LOG.fine("crs = " + crs);
                } catch (FactoryException e) {
                    SystemUtils.LOG.severe("Unknown CRS: " + tile.horizontalCsCode);
                }
            }

            L1cMetadata.TileGeometry selectedGeometry = tile.getGeometry(index);
            Envelope2D envelope = new Envelope2D(crs,
                                                 selectedGeometry.upperLeftX,
                                                 selectedGeometry.upperLeftY + selectedGeometry.numRows * selectedGeometry.yDim,
                                                 selectedGeometry.numCols * selectedGeometry.xDim,
                                                 -selectedGeometry.numRows * selectedGeometry.yDim);
            tileEnvelopes[i] = envelope;

            if (sceneEnvelope == null) {
                sceneEnvelope = new Envelope2D(crs, envelope);
            } else {
                sceneEnvelope.add(envelope);
            }
            tileInfos[i] = new TileInfo(i, tile.id, envelope, new Rectangle());
        }

        if (sceneEnvelope == null) {
            throw new IllegalStateException();
        }
        double imageX = sceneEnvelope.getX();
        double imageY = sceneEnvelope.getY() + sceneEnvelope.getHeight();
        Rectangle sceneBounds = null;
        for (int i = 0; i < tileEnvelopes.length; i++) {
            L1cMetadata.Tile tile = tileList.get(i);
            L1cMetadata.TileGeometry selectedGeometry = tile.getGeometry(index);
            Envelope2D tileEnvelope = tileEnvelopes[i];
            double tileX = tileEnvelope.getX();
            double tileY = tileEnvelope.getY() + tileEnvelope.getHeight();
            Rectangle rectangle = new Rectangle((int) ((tileX - imageX) / selectedGeometry.xDim),
                                                (int) ((imageY - tileY) / -selectedGeometry.yDim),
                                                selectedGeometry.numCols,
                                                selectedGeometry.numRows);
            if (sceneBounds == null) {
                sceneBounds = new Rectangle(rectangle);
            } else {
                sceneBounds.add(rectangle);
            }
            tileInfos[i] = new TileInfo(i, tile.id, tileEnvelope, rectangle);
        }

        return new L1cSceneDescription(tileInfos, sceneEnvelope, sceneBounds, index, config);
    }

    private L1cSceneDescription(TileInfo[] tileInfos,
                                Envelope2D sceneEnvelope,
                                Rectangle sceneRectangle,
                                L1cMetadata.Tile.idGeom geometry,
                                S2Config config) {
        super(config);

        this.tileInfos = tileInfos;
        this.sceneEnvelope = sceneEnvelope;
        this.sceneRectangle = sceneRectangle;
        this.geometry = geometry;
        this.tileInfoMap = new HashMap<String, TileInfo>();
        for (TileInfo tileInfo : tileInfos) {
            tileInfoMap.put(tileInfo.id, tileInfo);
        }
    }

    public Rectangle getSceneRectangle() {
        return sceneRectangle;
    }

    public Envelope2D getSceneEnvelope() {
        return sceneEnvelope;
    }

    public int getTileCount() {
        return tileInfos.length;
    }

    public String[] getTileIds() {
        final String[] tileIds = new String[tileInfos.length];
        for (int i = 0; i < tileInfos.length; i++) {
            tileIds[i] = tileInfos[i].id;
        }
        return tileIds;
    }

    public int getTileIndex(String tileId) {
        TileInfo tileInfo = tileInfoMap.get(tileId);
        return tileInfo != null ? tileInfo.index : -1;
    }

    public String getTileId(int tileIndex) {
        return tileInfos[tileIndex].id;
    }

    public Envelope2D getTileEnvelope(int tileIndex) {
        return tileInfos[tileIndex].envelope;
    }

    public Rectangle getTileRectangle(int tileIndex) {
        return tileInfos[tileIndex].rectangle;
    }

    public BufferedImage createTilePicture(int width) {

        Color[] colors = new Color[]{
                Color.GREEN,
                Color.RED,
                Color.BLUE,
                Color.YELLOW};

        double scale = width / sceneRectangle.getWidth();
        int height = (int) Math.round(sceneRectangle.getHeight() * scale);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.scale(scale, scale);
        graphics.translate(-sceneRectangle.getX(), -sceneRectangle.getY());
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setPaint(Color.WHITE);
        graphics.fill(sceneRectangle);
        graphics.setStroke(new BasicStroke(100F));
        graphics.setFont(new Font("Arial", Font.PLAIN, 800));

        for (int i = 0; i < tileInfos.length; i++) {
            Rectangle rect = tileInfos[i].rectangle;
            graphics.setPaint(addAlpha(colors[i % colors.length].brighter(), 100));
            graphics.fill(rect);
        }
        for (int i = 0; i < tileInfos.length; i++) {
            Rectangle rect = tileInfos[i].rectangle;
            graphics.setPaint(addAlpha(colors[i % colors.length].darker(), 100));
            graphics.draw(rect);
            graphics.setPaint(colors[i % colors.length].darker().darker());
            graphics.drawString("Tile " + (i + 1) + ": " + tileInfos[i].id,
                                rect.x + 1200F,
                                rect.y + 2200F);
        }
        return image;
    }

    private static Color addAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
