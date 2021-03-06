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
package savant.settings;

import java.awt.Font;
import java.net.URL;

import savant.util.NetworkUtils;


/**
 * Various constants and settings relevant to the browser as a whole.
 * @author mfiume
 */
public class BrowserSettings {

    private static PersistentSettings settings = PersistentSettings.getInstance();

    private static final String CHECKVERSION_KEY = "CHECKVERSION";
    private static final String COLLECTSTATS_KEY = "COLLECTSTATS";
    private static final String CACHINGENABLED_KEY = "CACHINGENABLED";
    private static final String SHOWSTARTPAGE_KEY = "SHOWSTARTPAGE";

    private static final Font TRACK_FONT = new Font("Sans-Serif", Font.BOLD, 12);
    
    /*
     * Remote Files
     */
    private static final String REMOTE_BUFFER_SIZE = "REMOTE_BUFFER_SIZE";
    public static final int DEFAULT_BUFFER_SIZE = 65536;

    /*
     * Website URLs
     */
    public static final URL URL = NetworkUtils.getKnownGoodURL("http://www.genomesavant.com/savant");
    public static final URL VERSION_URL = NetworkUtils.getKnownGoodURL(URL, "serve/version/version.xml");
    public static final URL GENOMES_URL = NetworkUtils.getKnownGoodURL(URL, "serve/data/genomes.xml");
    public static final URL DATA_URL = NetworkUtils.getKnownGoodURL(URL, "serve/data/data.xml");
    public static final URL PLUGIN_URL = NetworkUtils.getKnownGoodURL(URL, "serve/plugin/plugin.xml");
    public static final URL LOG_USAGE_STATS_URL = NetworkUtils.getKnownGoodURL(URL, "scripts/logUsageStats.cgi");
    public static final URL MEDIA_URL = NetworkUtils.getKnownGoodURL(URL, "media.html");
    public static final URL DOCUMENTATION_URL = NetworkUtils.getKnownGoodURL(URL, "documentation.html");
    public static final URL SHORTCUTS_URL = NetworkUtils.getKnownGoodURL(URL, "docs/SavantShortcuts.pdf");
    public static final URL NEWS_URL = NetworkUtils.getKnownGoodURL(URL, "serve/start/news.xml");
    public static final URL SAFE_URL = NetworkUtils.getKnownGoodURL(URL, "safe/savantsafe.php");

    public static final String VERSION = "2.0.4";
    public static String BUILD = "";

    /**
     * Is this version a beta release?
     */
    public static boolean isBeta() {
        return BUILD.startsWith("beta") || BUILD.startsWith("alpha");
    }

    /**
     * Does Savant need to check it's version number on startup?  Usually controlled by the
     * CHECKVERSION key, except in the case of beta releases, which always check.
     */
    public static boolean getCheckVersionOnStartup() {
        return isBeta() || settings.getBoolean(CHECKVERSION_KEY, true);
    }

    public static boolean getCollectAnonymousUsage() {
        return settings.getBoolean(COLLECTSTATS_KEY, true);
    }

    public static boolean getCachingEnabled() {
        return settings.getBoolean(CACHINGENABLED_KEY, true);
    }

    public static boolean getShowStartPage() {
        return settings.getBoolean(SHOWSTARTPAGE_KEY, true);
    }

    public static int getRemoteBufferSize(){
        String s = settings.getString(REMOTE_BUFFER_SIZE);
        return s != null ? Integer.parseInt(s) : DEFAULT_BUFFER_SIZE;
    }



    public static void setCheckVersionOnStartup(boolean b) {
        settings.setBoolean(CHECKVERSION_KEY, b);
    }

    public static void setCollectAnonymousUsage(boolean b) {
        settings.setBoolean(COLLECTSTATS_KEY, b);
    }

    public static void setCachingEnabled(boolean b) {
        settings.setBoolean(CACHINGENABLED_KEY, b);
    }

    public static void setRemoteBufferSize(int size){
        settings.setString(REMOTE_BUFFER_SIZE, String.valueOf(size));
    }

    public static void setShowStartPage(boolean b) {
        settings.setBoolean(SHOWSTARTPAGE_KEY, b);
    }

    public static Font getTrackFont() {
        return TRACK_FONT;
    }
}
