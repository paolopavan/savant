/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.controller.event;

/**
 *
 * @author mfiume
 */
public interface TrackRemovedListener
{
    public void trackRemovedEventReceived( TrackAddedOrRemovedEvent event );
}