/*
 *    Copyright 2010 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package savant.view.swing.interval;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.model.FileFormat;
import savant.model.GenericIntervalRecord;
import savant.model.Interval;
import savant.model.Resolution;
import savant.model.data.interval.GenericIntervalTrack;
import savant.model.view.AxisRange;
import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.model.view.Mode;
import savant.util.Range;
import savant.view.swing.BrowserDefaults;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.awt.*;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class IntervalViewTrack extends ViewTrack {

    private static Log log = LogFactory.getLog(IntervalViewTrack.class);

    public enum DrawingMode { SQUISH, PACK, ARC };

    private static final Mode SQUISH_MODE = new Mode(DrawingMode.SQUISH, "All on one line");
    private static final Mode PACK_MODE = new Mode(DrawingMode.PACK, "Minimum number of lines");
    private static final Mode ARC_MODE = new Mode(DrawingMode.ARC, "Arcs");

    public IntervalViewTrack(String name, GenericIntervalTrack intervalTrack) throws FileNotFoundException {
        super(name, FileFormat.INTERVAL_GENERIC, intervalTrack);
        setColorScheme(getDefaultColorScheme());
        setDrawModes(getDefaultDrawModes());
        setDrawMode(PACK_MODE);
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        //c.addColorSetting("BACKGROUND", BrowserDefaults.colorGraphMain);
        c.addColorSetting("TRANSLUCENT_GRAPH", new Color(0, 174, 255, 100));
        c.addColorSetting("OPAQUE_GRAPH", new Color(0,174,255));
        c.addColorSetting("LINE", BrowserDefaults.colorAccent);

        return c;
    }

    public Resolution getResolution(Range range) { return getResolution(range, getDrawMode()); }

    public Resolution getResolution(Range range, Mode mode)
    {
        if(mode.getName().equals("SQUISH")) {
            return getSquishModeResolution(range);
        } else if (mode.getName().equals("ARC")) {
            return getArcModeResolution(range);
        } else if (mode.getName().equals("PACK")) {
            return getDefaultModeResolution(range);
        } else {
            log.warn("Unrecognized draw mode " + mode.getName());
            return getDefaultModeResolution(range);
        }
    }

    public Resolution getDefaultModeResolution(Range range)
    {
        int length = range.getLength();

        return Resolution.VERY_HIGH;
    }

    public Resolution getArcModeResolution(Range range)
    {
        return Resolution.VERY_HIGH;
    }

    public Resolution getSquishModeResolution(Range range) {
        return Resolution.VERY_HIGH;
    }
    
    public List<Mode> getDefaultDrawModes()
    {
        List<Mode> modes = new ArrayList<Mode>();

        modes.add(SQUISH_MODE);
        modes.add(PACK_MODE);
        modes.add(ARC_MODE);
        return modes;
    }

    /**
     * getData
     *     Get data in the specified range at the specified resolution
     */
    public List<Object> retrieveData(Range range, Resolution resolution) {
        return new ArrayList<Object>(getTrack().getRecords(range, resolution));
    }

    public void prepareForRendering(Range range) throws Throwable {

        Resolution r = getResolution(range);

        List<Object> data = null;
        switch (r)
        {
            case VERY_HIGH:
                data = this.retrieveAndSaveData(range);
                break;
            default:
                break;
        }

        for (TrackRenderer renderer : getTrackRenderers()) {
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            if (getDrawMode().getName() == "ARC") {
                int maxDataValue = getMaxValue(data);
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, new AxisRange(range, new Range(0,(int)Math.round(Math.log(maxDataValue)))));
            }
            else renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, new AxisRange(range, getDefaultYRange()));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.MODE, getDrawMode());
            renderer.setData(data);
        }

    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    private int getMaxValue(List<Object>data) {
        double max = 0;
        for (Object o: data) {
            GenericIntervalRecord record = (GenericIntervalRecord)o;
            Interval interval = record.getInterval();
            double val = interval.getLength();
            if (val > max) max = val;
        }
        return (int)Math.ceil(max);
    }

    @Override
    public Mode getDefaultDrawMode() {
        return PACK_MODE;
    }
}
