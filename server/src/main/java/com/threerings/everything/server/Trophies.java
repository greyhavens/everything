//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2013 Three Rings Design, Inc.

package com.threerings.everything.server;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import com.threerings.everything.data.TrophyData;

public class Trophies {

    public static class TrophyRecord
    {
        public final ImmutableSet<Integer> sets;
        public final ImmutableMap<Integer, TrophyData> trophies;

        /**
         * Construct a set of related trophies.
         *
         * Replacements: %n - number (of sets)
         *               %o - ordinal (position of trophy, starting with 1)
         *               %r - roman (oridnal in roman numerals)
         */
        protected TrophyRecord (
            ImmutableSet<Integer> sets, String trophyId, String name, String desc, int... sizes) {
            this.sets = sets;
            if (sizes.length == 0) {
                sizes = new int[] { sets.size() };
            }
            ImmutableMap.Builder<Integer, TrophyData> builder = ImmutableMap.builder();
            for (int ii = 0; ii < sizes.length; ii++) {
                builder.put(sizes[ii],
                    new TrophyData(
                        replace(trophyId, ii + 1, sizes[ii]),
                        replace(name, ii + 1, sizes[ii]),
                        replace(desc, ii + 1, sizes[ii])));
            }
            trophies = builder.build();
        }

        protected static String replace (String s, int ordinal, int number) {
            s = s.replace("%o", String.valueOf(ordinal));
            s = s.replace("%n", String.valueOf(number));
            s = s.replace("%r", toRoman(ordinal));
            return s;
        }

        protected static String toRoman (int number) {
            String roman = "";
            for (int ii = 0; ii < ROMAN_TIERS.length; ii++) {
                while (number >= ROMAN_TIERS[ii]) {
                    roman += ROMAN_NUMERALS[ii];
                    number -= ROMAN_TIERS[ii];
                }
            }
            return roman;
        }

        /** Standard roman numerals support up to 3999, but it is extremely unlikely that we ever
         * have more than 20 or so levels. */
        protected static final int[] ROMAN_TIERS = { 10, 9, 5, 4, 1 };
        protected static final String[] ROMAN_NUMERALS = { "X", "IX", "V", "IV", "I" };
//        /** Look ma, unicode characters for the precision. */
//        protected static final int[] ROMAN_TIERS = {
//            11, 10, 9, 8, 7, 6, 5, 4,
//            3, 2, 1 };
//        protected static final String[] ROMAN_NUMERALS = {
//            "\u216a", "\u2169", "\u2168", "\u2167", "\u2166", "\u2165", "\u2164", "\u2163",
//            "\u2162", "\u2161", "\u2160" };
    }

    /**
     * Trophy data. TODO: from database...
     */
    public static final List<TrophyRecord> trophies = ImmutableList.of(
        // a series of trophies awarded purely for completing numbers of sets?
        new TrophyRecord(null,
            "sets%n", "Completed %n", "Complete %n sets of any kind",
            1, 3, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200, 250),
//        new TrophyRecord(null,
//            "test%n", "Test %r", "Test test %n sets",
//            0, 1, 2, 3),
        // simple trophies requiring complete collection
        new TrophyRecord(
            ImmutableSet.of(311, 315, 322, 332),
            "presidents", "U.S. Presidents", "Collect all U.S. Presidents"),
        new TrophyRecord(
            ImmutableSet.of(430, 432, 434),
            "carnivore", "Carnivore", "Collect all the cuts of meat"),
        new TrophyRecord(
            ImmutableSet.of(154, 155, 156, 157, 158, 159, 160),
            "consoles", "Game Consoles", "Collect every generation of game console"),
        new TrophyRecord(
            ImmutableSet.of(350, 351, 352, 353),
            "us_states", "All 50 States", "Collect every US State"),
        new TrophyRecord(
            ImmutableSet.of(486, 488, 489, 490, 493),
            "simpsons", "All Simpsons", "Collect every Simpsons character"),
        new TrophyRecord(
            ImmutableSet.of(465, 479, 480, 481, 482, 483),
            "start_trek", "All Star Trek", "Collect every Star Trek set"),
        new TrophyRecord(
            ImmutableSet.of(526, 527, 528),
            "herbs", "All Herbs and Spices", "Collect all the Herbs and Spices"),
        // more complex trophies requiring subsets of the sets
        new TrophyRecord(
            ImmutableSet.of(114, 184, 188, 189, 199, 205, 211, 273),
            "sevens", "Sevens", "Collect seven 'Seven' series",
            7), // need 7 of 8
        // standard "bla I", "bla II", "bla III" series collections
        new TrophyRecord(
            ImmutableSet.of(145, 146, 495, 540),
            "bands%n", "Bands %r", "Collect %n Music Band sets",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(234, 437, 487),
            "albums%n", "Albums %r", "Collect %n Music Album sets",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(166, 177, 219, 231, 274, 276, 283, 290, 449, 461, 465, 479, 480, 481,
                482, 483, 486, 488, 489, 490, 493),
            "television%n", "Television %r", "Collect %n Television sets",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(17, 98, 181, 235, 249, 257, 285, 289, 306, 355, 357, 362),
            "mammals%n", "Mammals %r", "Collect %n sets of mammals",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(57, 97, 244, 312, 433, 441),
            "birds%n", "Birds %r", "Collect %n sets of birds",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(56, 71, 126, 127, 128),
            "insects%n", "Insects %r", "Collect %n sets of insects",
            3, 5, 8, 12, 17),
        new TrophyRecord(
            ImmutableSet.of(317, 318, 319, 323, 324, 333, 334, 335, 336, 337, 338, 339, 340),
            "chemistry%n", "Chemistry %r", "Collect %n sets of chemicals",
            3, 5, 8, 12, 17)
    );
}
