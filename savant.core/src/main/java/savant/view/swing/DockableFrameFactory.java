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
package savant.view.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JPanel;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.data.DataFormat;
import savant.controller.FrameController;


/**
 * Factory for creating dockable frames.  This come in three flavours: one for bookmarks,
 * one for tracks (including genome tracks), and one for GUI plugins.
 */
public class DockableFrameFactory {
    private static final Log LOG = LogFactory.getLog(DockableFrameFactory.class);

    /**
     * Factory method used to create the Bookmarks frame.
     *
     * @param name the frame's title
     * @param mode STATE_HIDDEN, STATE_FLOATING, STATE_AUTOHIDE, or STATE_FRAMEDOCKED
     * @param side DOCK_SIDE_EAST, DOCK_SIDE_WEST, DOCK_SIDE_SOUTH, DOCK_SIDE_NORTH, or DOCK_SIDE_CENTER
     * @return a newly-created frame with behaviour like our Bookmarks panel
     */
    public static DockableFrame createFrame(String name, int mode, int side) {
        DockableFrame frame = new DockableFrame(name, null);
        frame.setSlidingAutohide(true);
        frame.setInitMode(mode);
        frame.setInitSide(side);
        frame.add(new JPanel());
        frame.setPreferredSize(new Dimension(400, 400));
        frame.setAutohideWidth(400);
        frame.setAutohideHeight(400);
        return frame;
    }

    public static DockableFrame createGUIPluginFrame(String name) {
        DockableFrame f = createFrame(name, DockContext.STATE_AUTOHIDE, DockContext.DOCK_SIDE_SOUTH);
        f.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE );
        return f;
    }

    public static Frame createTrackFrame(DataFormat df) {

        final Frame frame = new Frame(df);
        
        frame.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_MAXIMIZE | DockableFrame.BUTTON_CLOSE );
        
        frame.setSlidingAutohide(false);
        frame.setInitMode(DockContext.STATE_FRAMEDOCKED);
        frame.setInitSide(DockContext.DOCK_SIDE_NORTH);
        
        frame.add(new JPanel());

        frame.setCloseAction(new Action() {
            private boolean isEnabled = true;
            private Map<String,Object> map = new HashMap<String,Object>();

            @Override
            public void actionPerformed(ActionEvent e) {
                FrameController.getInstance().closeFrame(frame, true);
            }

            @Override
            public Object getValue(String key) {
                if (key.equals(Action.NAME)) { return "Close"; }
                else { return map.get(key); }
            }

            @Override
            public void putValue(String key, Object value) {
                map.put(key, value);
            }

            @Override
            public void setEnabled(boolean b) {
                this.isEnabled = b;
            }

            @Override
            public boolean isEnabled() {
                return isEnabled;
            }

            @Override
            public void addPropertyChangeListener(PropertyChangeListener listener) {}

            @Override
            public void removePropertyChangeListener(PropertyChangeListener listener) {}
        });


        // TODO: this seems cyclical. What's going on here?
        JPanel panel = (JPanel)frame.getContentPane();
        panel.setLayout(new BorderLayout());
        panel.add(frame.getFrameLandscape());
        return frame;
    }
}
