/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.view.tracks;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.RichIntervalRecord;
import savant.api.data.Interval;
import savant.api.data.Record;
import savant.api.data.Block;
import savant.api.data.IntervalRecord;
import savant.api.data.Strand;
import savant.api.util.Resolution;
import savant.exception.RenderingException;
import savant.settings.BrowserSettings;
import savant.util.*;


/**
 * Renderer for interval tracks which have extra information (and possibly blocks)
 *
 * @author vwilliams, tarkvara
 */
public class RichIntervalTrackRenderer extends TrackRenderer {

    private DrawingMode mode;
    Resolution resolution;

    public RichIntervalTrackRenderer() {
    }
    
    @Override
    public void render(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        renderPreCheck();

        mode = (DrawingMode)instructions.get(DrawingInstruction.MODE);
        resolution = (Resolution)instructions.get(DrawingInstruction.RESOLUTION);

        if (mode == DrawingMode.STANDARD) {
            renderPackMode(g2, gp, resolution);
        } else if (mode == DrawingMode.SQUISH) {
            renderSquishMode(g2, gp, resolution);
        }

        if (data.isEmpty()) {
            throw new RenderingException("No data in range", RenderingException.INFO_PRIORITY);
        }
    }

    private void renderPackMode(Graphics2D g2, GraphPaneAdapter gp, Resolution resolution) throws RenderingException {

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);

        double unitWidth = gp.getUnitWidth();

        FontMetrics fm = g2.getFontMetrics();
        List<Record> stuffedRecords = new ArrayList<Record>();
        for (Record r : data) {
            RichIntervalRecord ir = (RichIntervalRecord) r;
            int padAmount = 0;
            if (ir.getName() != null) {
                padAmount = (int)((fm.stringWidth(ir.getName()) + 5) / unitWidth);
            }
            stuffedRecords.add(new StuffedIntervalRecord(ir, padAmount, 0));
        }

        IntervalPacker packer = new IntervalPacker(stuffedRecords);
        // TODO: when it becomes possible, choose an appropriate number for breathing room parameter
        List<List<IntervalRecord>> intervals = StuffedIntervalRecord.getOriginalIntervals(packer.pack(2));

        gp.setXRange(axisRange.getXRange());
        int maxYRange;
        int numIntervals = intervals.size();
        // Set the Y range to the closest value of 10, 20, 50, 100, n*100
        if (numIntervals <= 10) maxYRange = 10;
        else if (numIntervals <= 20) maxYRange = 20;
        else if (numIntervals <=50) maxYRange = 50;
        else if (numIntervals <= 100) maxYRange = 100;
        else maxYRange = numIntervals;
        gp.setYRange(new Range(0,maxYRange));

        //resize frame if necessary
        if (gp.needsToResize()) return;

        // scan the map of intervals and draw the intervals for each level
        for (int level=0; level<intervals.size(); level++) {

            List<IntervalRecord> intervalsThisLevel = intervals.get(level);

            for (IntervalRecord intervalRecord : intervalsThisLevel) {

                Interval interval = intervalRecord.getInterval();
                RichIntervalRecord bedRecord = (RichIntervalRecord) intervalRecord;

                renderGene(g2, gp, cs, bedRecord, interval, level);

            }

        }
    }

    private void renderGene(Graphics2D g2, GraphPaneAdapter gp, ColourScheme cs, RichIntervalRecord rec, Interval interval, int level) {

        // for the chevrons
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double unitWidth = gp.getUnitWidth();
        double unitHeight = gp.getUnitHeight();
        int offset = gp.getOffset();

        g2.setFont(BrowserSettings.getTrackFont());

        // chose the color for the strand
        Color fillColor;
        if ((Boolean)instructions.get(DrawingInstruction.ITEMRGB) && rec.getItemRGB() != null && !rec.getItemRGB().isNull()) {
            //if an RGB value was supplied, use it
            fillColor = rec.getItemRGB().createColor();
        } else {
            //otherwise use the default color value
            fillColor = cs.getColor(rec.getStrand() == Strand.FORWARD ? ColourKey.FORWARD_STRAND : ColourKey.REVERSE_STRAND);
        }

        // Set alpha if score is enabled
        if ((Boolean)instructions.get(DrawingInstruction.SCORE) && !Float.isNaN(rec.getScore())) {
            fillColor = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), getConstrainedAlpha((int)(rec.getScore() * 0.255)));
        }

        Color lineColor = cs.getColor(ColourKey.INTERVAL_LINE);
        Color textColor = cs.getColor(ColourKey.INTERVAL_TEXT);

        double startXPos = gp.transformXPos(interval.getStart());

        boolean isInsertion = false;

        // If length is 0, draw insertion rhombus.  It is drawn here so that the name can be drawn on top of it.
        if (interval.getLength() == 0) {
            Shape rhombus = drawInsertion(g2, gp.transformXPos(interval.getStart()), gp.transformYPos(0) - ((level + 1) * unitHeight) - gp.getOffset(), gp.getUnitWidth(), unitHeight);
            isInsertion = true;

            startXPos -= unitWidth * 0.5;   // So the name won't stomp on the leftward-pointing arrow of the insertion.
            recordToShapeMap.put(rec, rhombus);
        }

        // Draw the gene name, if possible.
        String geneName = rec.getName();
        if ((Boolean)instructions.get(DrawingInstruction.ALTERNATE_NAME)) {
            geneName = rec.getAlternateName();
        }

        int thickStart = rec.getThickStart();
        int thickEnd = rec.getThickEnd();
        if (thickStart >= thickEnd) {
            // No actual thickness.  So make sure we don't get a "flange".
            thickStart = thickEnd = -1;
        } else {
            // The +1 is necessary to get range to be a multiple of 3.
            thickEnd++;
        }
        double thickStartX = gp.transformXPos(thickStart);
        double thickEndX = gp.transformXPos(thickEnd);
        if (!isInsertion) {

            double yPos = gp.getHeight() - (unitHeight * level) - (unitHeight / 2) - offset;

            Area area = new Area();

            // for each block, draw a rectangle
            List<Block> blocks = rec.getBlocks();
            double chevronIntervalStart = gp.transformXPos(interval.getStart());

            if (blocks == null) {
                // When blocks are null, we fake it out by drawing a single block with the full interval of the feature.
                blocks = new ArrayList<Block>();
                blocks.add(Block.valueOf(0, interval.getLength()));
            }

            for (Block block : blocks) {

                chevronIntervalStart = Math.max(chevronIntervalStart, 0);

                int blockStart = interval.getStart() + block.getPosition();
                int blockEnd = blockStart + block.getSize();
                double x = gp.transformXPos(blockStart);
                double y = gp.getHeight() - (unitHeight * (level + 1)) - offset;

                double chevronIntervalEnd = x;

                chevronIntervalEnd = Math.min(chevronIntervalEnd, gp.getWidth());

                if (chevronIntervalStart < chevronIntervalEnd && chevronIntervalEnd >= 0 && chevronIntervalStart <= gp.getWidth()) {
                    g2.setColor(lineColor);
                    g2.draw(new Line2D.Double(chevronIntervalStart, yPos, chevronIntervalEnd, yPos));
                    drawChevrons(g2, chevronIntervalStart, chevronIntervalEnd,  yPos, unitHeight, rec.getStrand(), area);
                }

                double w = block.getSize() * unitWidth;
                double h = unitHeight;

                Shape blockShape;
                if (blockStart >= thickStart) {
                    if (blockEnd <= thickEnd) {
                        // Simplest case.  Entire block is thick (includes case where thickness is not relevant).
                        blockShape = new Rectangle2D.Double(x, y, w, h);
                    } else if (blockStart >= thickEnd) {
                        // Also simple.  Entire block is thin.
                        blockShape = new Rectangle2D.Double(x, y + h * 0.25, w, h * 0.5);
                    } else {
                        // Block starts thick but ends thin.
                        blockShape = MiscUtils.createPolygon(x, y,
                                                             thickEndX, y,
                                                             thickEndX, y + h * 0.25,
                                                             x + w, y + h * 0.25,
                                                             x + w, y + h * 0.75,
                                                             thickEndX, y + h * 0.75,
                                                             thickEndX, y + h,
                                                             x, y + h);
                    }
                } else {
                    if (blockEnd <= thickStart) {
                        // Entire block is thin.
                        blockShape = new Rectangle2D.Double(x, y + h * 0.25, w, h * 0.5);
                    } else if (blockEnd <= thickEnd) {
                        // Block starts thin but ends thick.
                        blockShape = MiscUtils.createPolygon(x, y + h * 0.25,
                                                             thickStartX, y + h * 0.25,
                                                             thickStartX, y,
                                                             x + w, y,
                                                             x + w, y + h,
                                                             thickStartX, y + h,
                                                             thickStartX, y + h * 0.75,
                                                             x, y + h * 0.75);
                    } else {
                        // Worst case.  Block starts thin, goes thick in the middle and ends thin.
                        blockShape = MiscUtils.createPolygon(x, y + h * 0.25,
                                                             thickStartX, y + h * 0.25,
                                                             thickStartX, y,
                                                             thickEndX, y,
                                                             thickEndX, y + h * 0.25,
                                                             x + w, y + h * 0.25,
                                                             x + w, y + h * 0.75,
                                                             thickEndX, y + h * 0.75,
                                                             thickEndX, y + h,
                                                             thickStartX, y + h,
                                                             thickStartX, y + h * 0.75,
                                                             x, y + h * 0.75);
                    }
                }
                g2.setColor(fillColor);
                g2.fill(blockShape);
                if (h > 4 && w > 4) {
                    g2.setColor(lineColor);
                    g2.draw(blockShape);
                }
                area.add(new Area(blockShape));

                chevronIntervalStart = x + w;
            }
            recordToShapeMap.put(rec, area);
        }

        if (geneName != null) {
            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics();
            drawFeatureLabel(g2, geneName, startXPos, gp.getHeight() - (level * unitHeight) - unitHeight * 0.5 + (fm.getHeight() - fm.getDescent()) * 0.5 - offset);
        }
    }

    private void drawChevrons(Graphics2D g2, double start, double end, double y, double height, Strand strand, Area area) {
        final int SCALE_FACTOR = 40;
        final int interval = ((int)height/SCALE_FACTOR+1) * SCALE_FACTOR;
        // start the drawing on the next multiple of interval
        int startPos;
        if (start != interval) {
            startPos = (int)start + interval - ((int)start % interval);
        } else {
            startPos = interval;
        }

        for (double x = startPos; x < end; x+=interval) {

            double arrowWidth = height * 0.25;
            if ((end - start) > arrowWidth) {
                Path2D.Double arrow = null;
                if (strand == Strand.FORWARD) {
                    if ((x-arrowWidth) > start) {
                        if (height > SCALE_FACTOR) {
                            g2.draw(new Line2D.Double(x, y, x - arrowWidth, y + arrowWidth + 1.0));
                            g2.draw(new Line2D.Double(x, y, x - arrowWidth, y - arrowWidth));
                        } else {
                            arrow = MiscUtils.createPolygon(x, y, x - arrowWidth, y + arrowWidth + 1.0, x - arrowWidth, y - arrowWidth);
                        }
                    }
                } else {
                    if ((x+arrowWidth) < end) {
                        if (height > SCALE_FACTOR) {
                            g2.draw(new Line2D.Double(x, y, x + arrowWidth, y + arrowWidth + 1.0));
                            g2.draw(new Line2D.Double(x, y, x + arrowWidth, y - arrowWidth));
                        } else {
                            arrow = MiscUtils.createPolygon(x, y, x + arrowWidth, y + arrowWidth + 1.0, x + arrowWidth, y - arrowWidth);
                        }
                    }
                }
                if (arrow != null) {
                    g2.fill(new Area(arrow));
                    if (area != null) {
                        area.add(new Area(arrow));
                    }
                }
            }
        }
    }

    private void renderSquishMode(Graphics2D g2, GraphPaneAdapter gp, Resolution resolution) throws RenderingException {

        // ranges, width, and height
        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);

        gp.setXRange(axisRange.getXRange());
        // y range is set where levels are sorted out, after block merging pass

        if (resolution == Resolution.HIGH) {


            // colours
            ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
            Color forwardColor = cs.getColor(ColourKey.FORWARD_STRAND);     // Color also used for no-strand.
            Color reverseColor = cs.getColor(ColourKey.REVERSE_STRAND);
            Color lineColor = cs.getColor(ColourKey.INTERVAL_LINE);

            // antialising, for the chevrons
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // first pass through the data, merging Blocks
            List<Interval> posStrandBlocks = new ArrayList<Interval>();
            List<Interval> negStrandBlocks = new ArrayList<Interval>();
            List<Interval> noStrandBlocks = new ArrayList<Interval>();
            for (Record record: data) {
                RichIntervalRecord bedRecord = (RichIntervalRecord)record;
                Strand strand = bedRecord.getStrand();

                if (strand == Strand.FORWARD) {
                    mergeBlocks(posStrandBlocks, bedRecord);
                } else if (strand == Strand.REVERSE) {
                    mergeBlocks(negStrandBlocks, bedRecord);
                } else if (strand == null) {
                    mergeBlocks(noStrandBlocks, bedRecord);
                }

            }

            // allocate levels to strands, using 1, 2, or 3 depending on which strands are present in the data
            int posStrandLevel = -1;
            int negStrandLevel = -1;
            int noStrandLevel = -1;
            int signedStrandCount = posStrandBlocks.size() + negStrandBlocks.size();
            int noStrandCount = noStrandBlocks.size();
            if (signedStrandCount > 0 && noStrandCount > 0) {
                // assign three levels
                posStrandLevel = 0;
                negStrandLevel = 1;
                noStrandLevel = 2;
                gp.setYRange(new Range(0,3));
            } else if (signedStrandCount > 0 && noStrandCount == 0) {
                posStrandLevel = 0;
                negStrandLevel = 1;
                gp.setYRange(new Range(0,2));
            } else if (signedStrandCount == 0 && noStrandCount > 0) {
                noStrandLevel = 0;
            }
            double unitHeight = gp.getUnitHeight();
            // display only a message if intervals will not be visible at this resolution
            if (unitHeight < 1) {
                throw new RenderingException("Increase vertical pane size", RenderingException.LOWEST_PRIORITY);
            }            

            for (Record record: data) {

                RichIntervalRecord bedRecord = (RichIntervalRecord)record;

                // we'll display different strands at different y positions
                Strand strand =  bedRecord.getStrand();
                int level;
                if (strand == Strand.FORWARD) level = posStrandLevel;
                else if (strand == Strand.REVERSE) level = negStrandLevel;
                else level = noStrandLevel;

                Interval interval = bedRecord.getInterval();

                int startXPos = (int)gp.transformXPos(interval.getStart());

                //If length is 0, draw insertion rhombus.
                if (interval.getLength() == 0) {

                    drawInsertion(g2, gp.transformXPos(interval.getStart()), gp.transformYPos(0) - ((level + 1) * unitHeight) - gp.getOffset(), gp.getUnitWidth(), unitHeight);
                    // draw nothing else for this interval
                    continue;
                }

                // draw a line in the middle, the full length of the interval
                double yPos = gp.transformYPos(level) - unitHeight * 0.5;
                g2.setColor(lineColor);
                int lineWidth = (int)(interval.getLength() * gp.getUnitWidth());
                if (lineWidth > 4) {
                    g2.draw(new Line2D.Double(startXPos, yPos, startXPos + lineWidth, yPos));
                    drawChevrons(g2, startXPos, startXPos + lineWidth,  yPos, unitHeight, strand, null);
                }

            }

            // Now draw all blocks
            drawBlocks(posStrandBlocks, posStrandLevel, gp, forwardColor, lineColor, g2);
            drawBlocks(negStrandBlocks, negStrandLevel, gp, reverseColor, lineColor, g2);
            drawBlocks(noStrandBlocks, noStrandLevel, gp, forwardColor, lineColor, g2);
        } else {
            throw new RenderingException("Zoom in to see genes/intervals", RenderingException.LOWEST_PRIORITY);
        }
    }

    private void mergeBlocks(List<Interval> intervals, RichIntervalRecord bedRecord) {

        List<Block> blocks = bedRecord.getBlocks();
        if (blocks == null) {
            // When blocks are null, we fake it out by drawing a single block with the full interval of the feature.
            // This occurs in snp tracks
            blocks = new ArrayList<Block>();
            blocks.add(Block.valueOf(0, bedRecord.getInterval().getLength()-1));
        }
        Interval gene = bedRecord.getInterval();
        if (intervals.isEmpty()) {
            for (Block block: blocks) {
                int blockStart = gene.getStart() + block.getPosition();
                Interval blockInterval = Interval.valueOf(blockStart, blockStart+block.getSize());
                intervals.add(blockInterval);
            }
        } else {
            for (Block block: blocks) {
                // merging only works on intervals, so convert block to interval
                int blockStart = gene.getStart() + block.getPosition();
                Interval blockInterval = Interval.valueOf(blockStart, blockStart+block.getSize());
                ListIterator<Interval> intervalIt = intervals.listIterator();
                boolean merged = false;
                while (intervalIt.hasNext() && !merged) {
                    Interval interval = intervalIt.next();
                    if (blockInterval.intersectsOrAbuts(interval)) {
                        intervalIt.set(blockInterval.merge(interval));
                        merged = true;
                    }
                }
                if (!merged) {
                    intervals.add(blockInterval);
                }
            }
        }
        
    }

    private void drawBlocks(List<Interval> blocks, int level, GraphPaneAdapter gp, Color fillColor, Color lineColor, Graphics2D g2) {

        if (blocks == null || blocks.isEmpty()) return;

        double unitWidth = gp.getUnitWidth();
        double unitHeight = gp.getUnitHeight();
        double x, y, w, h;
        for (Interval block: blocks) {
            if (block.getLength() == 0) continue;
            x = gp.transformXPos(block.getStart());
            y = gp.transformYPos(level) - unitHeight;
            w = block.getLength() * unitWidth;
            h = unitHeight;

            Rectangle2D.Double blockRect = new Rectangle2D.Double(x,y,w,h);

            g2.setColor(fillColor);
            g2.fill(blockRect);
            if (h > 4 && w > 4) {
                g2.setColor(lineColor);
                g2.draw(blockRect);
            }
        }
    }
    
    @Override
    public Dimension getLegendSize(DrawingMode mode) {
        switch (mode) {
            case STANDARD:
                return new Dimension(150, LEGEND_LINE_HEIGHT * 4 + 6);
            case SQUISH:
                return new Dimension(150, LEGEND_LINE_HEIGHT * 3 + 6);
            default:
                return null;
        }
    }

    @Override
    public void drawLegend(Graphics2D g2, DrawingMode mode) {
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        int x = 6, y = 17;
        
        g2.setColor(cs.getColor(ColourKey.FORWARD_STRAND));
        g2.fillRect(x, y - 10, 36, 12);
        g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
        g2.drawRect(x, y - 10, 36, 12);
        g2.setColor(Color.BLACK);
        g2.setFont(LEGEND_FONT);
        g2.drawString(ColourKey.FORWARD_STRAND.getName(), x + 45, y);

        y += LEGEND_LINE_HEIGHT;
        g2.setColor(cs.getColor(ColourKey.REVERSE_STRAND));
        g2.fillRect(x, y - 10, 36, 12);
        g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
        g2.drawRect(x, y - 10, 36, 12);
        g2.setColor(Color.BLACK);
        g2.drawString(ColourKey.REVERSE_STRAND.getName(), x + 45, y);
        
        y += LEGEND_LINE_HEIGHT - 3;
        g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
        g2.drawLine(x, y, x + 36, y);
        g2.fill(MiscUtils.createPolygon(x + 15, y - 4, x + 19, y, x + 15, y + 5));
        y += 3;
        g2.setColor(Color.BLACK);
        g2.drawString("Intron", x + 45, y);
        

        if (mode == DrawingMode.STANDARD) {
            y += LEGEND_LINE_HEIGHT;
            g2.setColor(cs.getColor(ColourKey.FORWARD_STRAND));
            g2.fillRect(x, y - 8, 36, 6);
            g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
            g2.drawRect(x, y - 8, 36, 6);
            g2.setColor(Color.BLACK);
            g2.drawString("Non-coding", x + 45, y);
        }
    }
}
