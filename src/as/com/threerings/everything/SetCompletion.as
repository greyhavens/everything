//
// $Id$

package com.threerings.everything {

import flash.events.Event;

import flash.display.Sprite;

import flash.utils.getTimer; // function import

/**
 * Display some fireworks when a set is completed.
 */
[SWF(width="500", height="500")]
public class SetCompletion extends Sprite
{
    public static const MIN_FREQUENCY :int = 100;
    public static const MAX_FREQUENCY :int = 500;

    public static const WIDTH :int = 500;
    public static const HEIGHT :int = 500;

    public static const MIN_RADIUS :int = 100;
    public static const MAX_RADIUS :int = 200;

    public static const COLORS :Array = [
        0xFF0000, 0x00FF00, 0x0000FF,
        0xFFFF00, 0xFF00FF, 0x00FFFF,
        0xFF9900, 0xFF0099, 0x00FF99, 0x0099FF, 0x99FF00, 0x9900FF
        ];

    public static const MIN_DURATION :int = 800;
    public static const MAX_DURATION :int = 1600;

    public static const MIN_SPARKS :int = 20;
    public static const MAX_SPARKS :int = 30;

    public function SetCompletion ()
    {
        addEventListener(Event.ENTER_FRAME, handleFrame);
    }

    /**
     * Return a random int between min (inclusive) and max (exclusive).
     */
    public static function random (min :int, max :int) :int
    {
        return Math.floor(Math.random() * (max - min)) + min;
    }

    protected function handleFrame (... ignored) :void
    {
        var now :Number = getTimer();

        if (now > _nextWork) {
            addWork(now);
            _nextWork = now + random(MIN_FREQUENCY, MAX_FREQUENCY);
        }

        for (var ii :int = _works.length - 1; ii >= 0; ii--) {
            var work :Work = Work(_works[ii]);
            if (work.update(now)) {
                removeChild(work);
                _works.splice(ii, 1);
            }
        }
    }

    protected function addWork (now :Number) :void
    {
        var color :uint = uint(COLORS[random(0, COLORS.length)]);
        var duration :Number = random(MIN_DURATION, MAX_DURATION);
        var sparks :int = random(MIN_SPARKS, MAX_SPARKS);

        var radius :Number = random(MIN_RADIUS, MAX_RADIUS);

        var work :Work = new Work(now, duration, radius, color, sparks);
        work.x = random(radius, WIDTH - radius);
        work.y = random(radius, HEIGHT - radius);
        _works.push(work);
        addChild(work);
    }

    protected var _nextWork :Number = 0;
    protected var _works :Array = [];
}
}

import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import com.threerings.everything.SetCompletion;

/**
 * One firework.
 */
class Work extends Sprite
{
    public function Work (
        now :Number, duration :Number, radius :Number, color :uint, sparks :int)
    {
        _startStamp = now;
        _duration = duration;
        _radius = radius;

        var g :Graphics;
        for (var ii :int = 0; ii < sparks; ii++) {
            var spark :Sprite = new Sprite();
            g = spark.graphics;
            g.beginFill(color);
            g.drawCircle(0, 0, 1.8);
            g.endFill();
            _sparks.push(spark);

            var trail :Sprite = new Sprite();
            g = trail.graphics;
            g.beginFill(color);
            g.drawRect(-1, -1, 1, 1);
            g.endFill();
            _trails.push(trail);

            var holder :Sprite = new Sprite();
            holder.rotation = (ii * 360) / sparks;
            addChild(holder);
            holder.addChild(trail);
            holder.addChild(spark);
        }
    }

    /**
     * Return true when we're ready to die.
     */
    public function update (stamp :Number) :Boolean
    {
        var complete :Number = (stamp - _startStamp) / _duration;
        if (complete > 1) {
            return true;
        }

        var cubicOut :Number = complete - 1;
        cubicOut = cubicOut*cubicOut*cubicOut+1;
        var cubicIn :Number = complete*complete*complete;

        var sparkY :Number = _radius * cubicOut;
        var trailAlpha :Number = .8 - (complete * .8);
        var trailScale :Number = (1 - cubicOut) * sparkY;

        for each (var spark :Sprite in _sparks) {
            spark.y = sparkY;
        }
        for each (var trail :Sprite in _trails) {
            trail.alpha = trailAlpha;
            trail.y = sparkY;
            trail.scaleY = trailScale;
        }

        if (complete >= .75) {
            this.alpha = 1 - ((complete - .75) / .25);
        }
        return false;
    }

    protected var _startStamp :Number;
    protected var _duration :Number;
    protected var _radius :Number;

    protected var _sparks :Array = [];
    protected var _trails :Array = [];
}
