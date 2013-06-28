//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2010-2012 Three Rings Design, Inc.

package com.threerings.everything.client.game;

import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.everything.data.Grid;
import com.threerings.gwt.ui.FX;

/**
 * Used to render our card table.
 */
public abstract class Table
{
    /** The number of card columns. */
    public static final int COLUMNS = 4;

    /** The number of card rows. */
    public static final int ROWS = Grid.SIZE/COLUMNS;

    /** The width of a card column in pixels. */
    public static final int COL_WIDTH = 142;

    /** The height of a card column in pixels. */
    public static final int ROW_HEIGHT = 167;

    /** Used to animate cards into position on the table. */
    public static class Animator
    {
        /** Used to map a column and row to an integer value. */
        public interface F {
            public int apply (int col, int row);
        }

        /** Returns the starting x position of the card at the specified column and row. */
        public final F startx;

        /** Returns the starting y position of the card at the specified column and row. */
        public final F starty;

        public Animator (F startx, F starty, F duration, F delay) {
            this.startx = startx;
            this.starty = starty;
            _duration = duration;
            _delay = delay;
        }

        /** Starts the card animation. */
        public void animate (AbsolutePanel panel, Widget target, int col, int row) {
            int sx = startx.apply(col, row), sy = starty.apply(col, row);
            int dx = col * COL_WIDTH, dy = row * ROW_HEIGHT;
            double delay = System.currentTimeMillis() + _delay.apply(col, row);
            FX.move(panel, target).from(sx, sy).to(dx, dy).run(_duration.apply(col, row), delay);
        }

        protected final F _duration;
        protected final F _delay;
    }

    /**
     * Selects a random card animation.
     */
    public static Animator pickAnimation (boolean animate)
    {
        return animate ? new Animator(COL_FUNCS[Random.nextInt(COL_FUNCS.length)],
                                      ROW_FUNCS[Random.nextInt(ROW_FUNCS.length)],
                                      DUR_FUNCS[Random.nextInt(DUR_FUNCS.length)],
                                      DELAY_FUNCS[Random.nextInt(DELAY_FUNCS.length)]) :
            new Animator(COL_FUNC, ROW_FUNC, constant(0), constant(0)) {
                public void animate (AbsolutePanel panel, Widget target, int col, int row) {
                    // nada!
                }
            };
    }

    protected static Animator.F constant (final int value)
    {
        return new Animator.F() {
            public int apply (int col, int row) {
                return value;
            }
        };
    }

    protected static final Animator.F ROW_FUNC = new Animator.F() {
        public int apply (int col, int row) {
            return row * ROW_HEIGHT;
        }
    };

    protected static final Animator.F COL_FUNC = new Animator.F() {
        public int apply (int col, int row) {
            return col * COL_WIDTH;
        }
    };

    protected static final Animator.F[] COL_FUNCS = new Animator.F[] {
        COL_FUNC, // default column
        constant(0), // left column
        constant((COLUMNS-1) * COL_WIDTH), // right column
        constant((COLUMNS-1) * COL_WIDTH/2), // center
    };

    protected static final Animator.F[] ROW_FUNCS = new Animator.F[] {
        ROW_FUNC, // default row
        constant(0), // top row
        constant((ROWS-1) * ROW_HEIGHT), // bottom row
        constant((ROWS-1) * ROW_HEIGHT/2), // center
    };

    protected static final Animator.F[] DUR_FUNCS = new Animator.F[] {
        constant(1000), // default
    };

    protected static final Animator.F[] DELAY_FUNCS = new Animator.F[] {
        constant(0), // default
        ROW_FUNC, // delay based on row
        COL_FUNC, // delay based on column
    };
}
