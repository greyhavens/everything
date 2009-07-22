//
// $Id$

package com.threerings.everything {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.MovieClip;
import flash.display.Sprite;
import flash.display.StageScaleMode;
import flash.events.MouseEvent;
import flash.external.ExternalInterface;
import flash.system.ApplicationDomain;
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
        stage.scaleMode = StageScaleMode.NO_SCALE;
        if (ExternalInterface.available) {
            ExternalInterface.addCallback("setValue", setValue);
        }
        MultiLoader.loadClasses([UI], _contentDomain, onReady);
    }

    protected function onReady () :void
    {
        // extract some critical bits
        _id = (root.loaderInfo.parameters["id"] as String);
        _type = (root.loaderInfo.parameters["type"] as String);
        _type = (_type == null) ? "card_back" : _type; // for testing

        // create our clip
        _clip = instantiateClip(_type);
        _clip.x = 50;
        _clip.y = 50;
        addChild(_clip);
        trace("Loaded clip " + _clip.width + "x" + _clip.height);

        // TODO: have GWT tell us if we should listen for clicks
        if (_type == "card_front" || _type == "card_back") {
            // TODO: change cursor when we're hoverable
            _clip.addEventListener(MouseEvent.CLICK, onClick);
        }

        // fill in our parameters
        for (var key :String in root.loaderInfo.parameters) {
            if (key == "id" || key == "type") {
                // nada
            } else {
                setValue(key, root.loaderInfo.parameters[key]);
            }
        }
    }

    protected function setValue (name :String, value :String) :void
    {
        // trace("Setting " + name + " -> " + value);
        if (name == "image") {
            var have :DisplayObject = _clip[name] as DisplayObject;
            if (have == null) {
                trace("Hrm, couldn't find anchor '" + name + "'.");
                return;
            }

            var cx :int = have.x + have.width/2, cy :int = have.y + have.height/2;
            var hparent :DisplayObjectContainer = have.parent;
            hparent.removeChild(have);

            if (value != "") {
                trace("Am I an info card " + _type);
                var image :Image = (_type == "info_card") ?
                    new Image(value, 250, 300) : new Image(value, 83, 100);
                image.x = cx;
                image.y = cy;
                hparent.addChild(image);
            }

        } else if (name == "descrip") {
            _descrip = value;
            maybeFormatInfo();

        } else if (name == "facts") {
            _facts = value;
            maybeFormatInfo();

        } else if (name == "source") {
            setHTML(name, "Source: <a target=\"_blank\" href=\"" + value + "\">" +
                    nameSource(value) + "</a>");

        } else {
            setText(name, value);
        }
    }

    protected function maybeFormatInfo () :void
    {
        if (_descrip != null && _facts != null) {
            setHTML("information", _descrip + "<br/><b>Facts:</b>" + formatFacts(_facts));
        }
    }

    protected function setText (name :String, text :String) :void
    {
        if (_clip[name] == null) {
            trace("Missing '" + name + "' component. Can't set text.");
        } else {
            _clip[name].text = text;
        }
    }

    protected function setHTML (name :String, html :String) :void
    {
        if (_clip[name] == null) {
            trace("Missing '" + name + "' component. Can't set HTML.");
        } else {
            _clip[name].htmlText = html;
        }
    }

    protected function onClick (event :MouseEvent) :void
    {
        if (ExternalInterface.available) {
            ExternalInterface.call("cardClicked", _id);
        } else {
            trace("Can't dispatch click. No external interface.");
        }
    }

    protected function instantiateClip (symbolName :String) :MovieClip
    {
        var symbolClass :Class = _contentDomain.getDefinition(symbolName) as Class;
        return MovieClip(new symbolClass());
    }

    protected static function nameSource (source :String) :String
    {
        if (source.indexOf("wikipedia.org") != -1) {
            return "Wikipedia";
        }

        var ssidx :int = source.indexOf("//");
        var eidx :int = source.indexOf("/", ssidx+2);
        if (ssidx == -1) {
            return source;
        } else if (eidx == -1) {
            return source.substring(ssidx+2);
        } else {
            return source.substring(ssidx+2, eidx);
        }
    }

    protected static function formatFacts (facts :String) :String
    {
        var buf :String = "<ul>";
        for each (var bit :String in facts.split("\n")) {
            buf += ("<li>" + bit + "</li>");
        }
        buf += "</ul>";
        return buf;
    }

    protected var _contentDomain :ApplicationDomain = new ApplicationDomain(null);
    protected var _clip :MovieClip;
    protected var _id :String;
    protected var _type :String;
    protected var _descrip :String;
    protected var _facts :String;

    [Embed(source="ui.swf", mimeType="application/octet-stream")]
    protected var UI :Class;
}
}
