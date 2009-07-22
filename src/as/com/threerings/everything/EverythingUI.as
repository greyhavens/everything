//
// $Id$

package com.threerings.everything {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.MovieClip;
import flash.display.Sprite;
import flash.display.StageScaleMode;
import flash.external.ExternalInterface;
import flash.system.ApplicationDomain;
import flash.system.Security;
import flash.text.TextField;

import com.threerings.util.MultiLoader;

/**
 * The main entry point for the Everything Flash UI.
 */
[SWF(width="100", height="100")]
public class EverythingUI extends Sprite
{
    public function EverythingUI ()
    {
        // stage.scaleMode = StageScaleMode.SHOW_ALL;
        stage.scaleMode = StageScaleMode.NO_SCALE;
        if (ExternalInterface.available) {
            ExternalInterface.addCallback("setValue", setValue);
        }
        // Security.loadPolicyFile(S3_BUCKET + "crossdomain.xml");
        MultiLoader.loadClasses([UI], _contentDomain, onReady);
    }

    protected function onReady () :void
    {
        // create our clip
        var type :String = (root.loaderInfo.parameters["type"] as String);
        if (type == null) {
            type = "card_back";
        }
        _clip = instantiateClip(type);
        _clip.x = 50;
        _clip.y = 50;
        addChild(_clip);

        // fill in our parameters
        for (var key :String in root.loaderInfo.parameters) {
            setValue(key, root.loaderInfo.parameters[key]);
        }
    }

    protected function setValue (name :String, value :String) :void
    {
        // trace("Setting " + name + " -> " + value);
        if (name == "type") {
            // nada

        } else if (name == "image") {
            var have :DisplayObject = _clip[name] as DisplayObject;
            if (have == null) {
                trace("Hrm, couldn't find anchor '" + name + "'.");
                return;
            }

            var cx :int = have.x + have.width/2, cy :int = have.y + have.height/2;
            var hparent :DisplayObjectContainer = have.parent;
            hparent.removeChild(have);

            if (value != "") {
                var image :Image = new Image(value, 83, 100);
                image.x = cx;
                image.y = cy;
                hparent.addChild(image);
            }

        } else {
            _clip[name].text = value;
        }
    }

    protected function setImage (name :String, url :String) :void
    {
        _clip[name].text = url; // TODO
    }

    protected function instantiateClip (symbolName :String) :MovieClip
    {
        var symbolClass :Class = _contentDomain.getDefinition(symbolName) as Class;
        return MovieClip(new symbolClass());
    }

    protected var _contentDomain :ApplicationDomain = new ApplicationDomain(null);
    protected var _clip :MovieClip;

    [Embed(source="ui.swf", mimeType="application/octet-stream")]
    protected var UI :Class;
}
}
