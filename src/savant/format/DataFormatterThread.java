package savant.format;

import savant.view.dialog.FormatFrame;

public class DataFormatterThread implements Runnable {

    private DataFormatter df;
    private FormatFrame ff;

    public DataFormatterThread(DataFormatter df) {
        this.df = df;
    }

    public void setFormatFrame(FormatFrame ff) {
        this.ff = ff;
    }

    /*
    public boolean didFormattingComplete() {
        return this.didComplete;
    }
     * 
     */

    @Override
    public void run() {
        try {
            df.format();
            notifyFormatFrameOfTermination(true, null);
        } catch (Throwable ex) {
            notifyFormatFrameOfTermination(false, ex);
        }
    }

    private void notifyFormatFrameOfTermination(boolean wasFormatSuccessful, Throwable e) {
        if (ff != null) {
            ff.notifyOfTermination(wasFormatSuccessful, e);
        }
    }
}