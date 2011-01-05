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

package savant.view.swing.point;

import java.io.IOException;
import java.util.List;

import savant.api.adapter.RangeAdapter;
import savant.data.sources.DataSource;
import savant.data.types.Record;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ColourSettings;
import savant.util.*;
import savant.view.swing.Track;


/**
 *
 * @author mfiume
 */
public class PointTrack extends Track {

    public List<Record> savedList = null;

    public PointTrack(DataSource dataSource) throws SavantTrackCreationCancelledException {
        super(dataSource, new PointTrackRenderer());

        setColorScheme(getDefaultColorScheme());
        this.notifyControllerOfCreation();
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("Background", ColourSettings.getPointFill());
        c.addColorSetting("Line", ColourSettings.getPointLine());

        return c; 
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
    }

    @Override
    public Resolution getResolution(RangeAdapter range)
    {
        long length = range.getLength();

        if (length > 100000) { return Resolution.VERY_LOW; }
        return Resolution.VERY_HIGH;
    }

    @Override
    public void prepareForRendering(String reference, Range range) {
        Resolution r = getResolution(range);

        List<Record> data = null;

        switch (r) {
            case VERY_HIGH:
                renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading track...");
                requestData(reference, range);
                break;
            default:
                saveNullData();
                break;
        }

        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, this.getColorScheme());
        renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }
}