//
// $Id$

package com.threerings.everything {

import flash.display.Sprite;
import flash.text.TextField;

/**
 * The main entry point for the Everything Flash UI.
 */
public class EverythingUI extends Sprite
{
    public function EverythingUI ()
    {
        var text :TextField = new TextField();
        text.text = "Abandon slightly less hope as you embark upon this path.";
        addChild(text);
    }
}
}
