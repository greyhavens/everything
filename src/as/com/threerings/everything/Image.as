//
// $Id$

package com.threerings.everything {

import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;
import flash.events.SecurityErrorEvent;
import flash.net.URLRequest;

/**
 * Loads and displays an image, scaling it to fit into a specified max width and height.
 */
public class Image extends Loader
{
    public function Image (url :String, maxwidth :int, maxheight :int)
    {
        _maxwidth = maxwidth;
        _maxheight = maxheight;

        contentLoaderInfo.addEventListener(Event.COMPLETE, handleComplete);
        contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        contentLoaderInfo.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleError);
        contentLoaderInfo.addEventListener(ProgressEvent.PROGRESS, handleProgress);

        trace("Loading " + url);
        load(new URLRequest(S3_BUCKET + url));
        maybeUpdateDimensions(contentLoaderInfo);
    }

    protected function handleError (event :ErrorEvent) :void
    {
        trace("Oh noez: " + event);
    }

    protected function handleComplete (event :Event) :void
    {
        maybeUpdateDimensions(contentLoaderInfo);
    }

    protected function handleProgress (event :ProgressEvent) :void
    {
        maybeUpdateDimensions(event.target as LoaderInfo);
    }

    protected function maybeUpdateDimensions (info :LoaderInfo) :void
    {
        try {
            trace("Got dimensions " + info.width + "x" + info.height +
                  " (self " + width + "x" + height + ")");

            var wf :Number = info.width / _maxwidth;
            var hf :Number = info.height / _maxheight;

            if (wf > 1 && wf > hf) {
                scaleX = 1/wf;
                scaleY = 1/wf;
            } else if (hf > 1 && hf > wf) {
                scaleX = 1/hf;
                scaleY = 1/hf;
            }

            x -= width/2;
            y -= height/2;

            trace("Scaled and positioned " + width + "x" + height + x + y);

        } catch (err :Error) {
            // an error is thrown trying to access these props before they're ready
        }
    }

    protected var _maxwidth :int, _maxheight :int;

    /** The URL via which we load images from our Amazon S3 bucket. */
    protected static const S3_BUCKET :String = "http://s3.amazonaws.com/everything.threerings.net/";
}
}
