//
// $Id$

package com.threerings.everything {

import mx.containers.Panel;
import mx.containers.ViewStack;
import mx.controls.Label;
import mx.core.Application;

/**
 * The root of the Everything UI.
 */
public class EverythingPanel extends Panel
{
    public function EverythingPanel (app :Application, parent :ViewStack)
    {
        var label :Label = new Label();
        label.text = "Abaondon hope all ye who set down this path.";
        addChild(label);
    }
}
}
