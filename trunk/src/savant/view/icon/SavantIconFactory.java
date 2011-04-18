/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.icon;

import javax.swing.ImageIcon;

/**
 *
 * @author Marc Fiume
 */
public class SavantIconFactory {

    static SavantIconFactory instance;

    public SavantIconFactory() {
        instance = this;
    }

    public static SavantIconFactory getInstance() {
        if (instance == null) {
            instance = new SavantIconFactory();
        }
        return instance;
    }

    public ImageIcon getIcon(String resourcePath) {
        return new ImageIcon(getClass().getResource(resourcePath));
    }

    public enum StandardIcon { 
        ADD,
        SAVE,
        OPEN,
        COPY,
        CLOSE,
        DELETE,
        RUN,
        STOP,
        REFRESH,
        UP,
        DOWN,
        UNDO,
        REDO,
        ZOOMIN,
        ZOOMOUT,
        SHIFT_FARLEFT,
        SHIFT_FARRIGHT,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        VIEW,
        LOGO,
        TRACK,
        FOLDER,
        BKMK_ADD,
        BKMK_RM,
        PLUGIN };

    public ImageIcon getIcon(StandardIcon icon) {
        switch(icon) {
            case ADD:
                return getIcon("/savant/images/icon/add.png");
            case SAVE:
                return getIcon("/savant/images/icon/save2.png");
            case OPEN:
                return getIcon("/savant/images/icon/open.png");
            case COPY:
                return getIcon("/savant/images/icon/copy2.png");
            case CLOSE:
                return getIcon("/savant/images/icon/close_red.png");
            case DELETE:
                return getIcon("/savant/images/icon/delete.png");
            case RUN:
                return getIcon("/savant/images/icon/run.png");
            case UNDO:
                return getIcon("/savant/images/icon/undo.png");
            case REDO:
                return getIcon("/savant/images/icon/redo.png");
            case ZOOMIN:
                return getIcon("/savant/images/icon/in.png");
            case ZOOMOUT:
                return getIcon("/savant/images/icon/out.png");
            case SHIFT_FARLEFT:
                return getIcon("/savant/images/icon/leftfull.png");
            case SHIFT_FARRIGHT:
                return getIcon("/savant/images/icon/rightfull.png");
            case SHIFT_LEFT:
                return getIcon("/savant/images/icon/left.png");
            case SHIFT_RIGHT:
                return getIcon("/savant/images/icon/right.png");
            case STOP:
                return getIcon("/savant/images/icon/stop.png");
            case REFRESH:
                return getIcon("/savant/images/icon/refresh.png");
            case UP:
                return getIcon("/savant/images/icon/up.png");
            case DOWN:
                return getIcon("/savant/images/icon/down.png");
            case VIEW:
                return getIcon("/savant/images/icon/view.png");
            case FOLDER:
                return getIcon("/savant/images/icon/folder.png");
            case LOGO:
                return getIcon("/savant/images/icon/logo.png");
            case TRACK:
                return getIcon("/savant/images/icon/track.png");
            case BKMK_ADD:
                return getIcon("/savant/images/icon/bkm_add.png");
            case BKMK_RM:
                return getIcon("/savant/images/icon/bkm_rm.png");
            case PLUGIN:
                return getIcon("/savant/images/icon/plugin.png");
            default:
                return null;
        }
    }

}
