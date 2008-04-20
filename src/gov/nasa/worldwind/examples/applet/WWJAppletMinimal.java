package gov.nasa.worldwind.examples.applet;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.examples.StatusBar;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.Earth.BMNGOneImage;

import javax.swing.*;
import java.awt.*;

/**
 * Provides a base application framework for simple WorldWind applets.
 *
 * A simple applet which runs World Wind with a StatusBar at the bottom
 *
 * @author Patrick Murris
 * @version $Id:
 */

public class WWJAppletMinimal extends JApplet {
    
    private WorldWindowGLCanvas wwd;
    private StatusBar statusBar;

    public WWJAppletMinimal()
    {
    }

    public void init()
    {
        try
        {
            // Create World Window GL Canvas
            this.wwd = new WorldWindowGLCanvas();
            this.getContentPane().add(this.wwd, BorderLayout.CENTER);

            // Create the default model as described in the current worldwind properties.
            Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            this.wwd.setModel(m);

            // Add a BMNG base layer to the model layer list, before the Blue Marble
            insertBeforeLayerName(this.wwd, new BMNGOneImage(), "Blue Marble");

            // Add the status bar
            this.statusBar = new StatusBar();
            this.getContentPane().add(this.statusBar, BorderLayout.PAGE_END);

            // Forward events to the status bar to provide the cursor position info.
            this.statusBar.setEventSource(this.wwd);

        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    public void stop()
    {
        // Shut down World Wind
        WorldWind.shutDown();
    }

    /**
     * Adds a layer to WW current layerlist, before a named layer
     * Target name can be a part of the layer name
     * @param wwd
     * @param layer
     * @param targetName
     */
    public static void insertBeforeLayerName(WorldWindow wwd, Layer layer, String targetName)
    {
        // Insert the layer into the layer list just before the target layer.
        int targetPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l.getName().indexOf(targetName) != -1)
            {
                targetPosition = layers.indexOf(l);
                break;
            }
        }
        layers.add(targetPosition, layer);
    }

}
