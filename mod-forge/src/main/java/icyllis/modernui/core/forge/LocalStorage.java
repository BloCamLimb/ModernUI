/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.core.forge;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Save and load data from local game files.
 * There are different work on the server and the client.
 */
//TODO functional
public final class LocalStorage {

    private static final Object2IntMap<String> EMOJI_MAP = new Object2IntOpenHashMap<>();
    private static final List<Pair<String, Integer>> EMOJI_HISTORY = new ArrayList<>();

    static void init() {

    }

    // Unstable
    public static boolean checkOneTimeEvent(int mask) {
        int v = Config.COMMON.oneTimeEvents.get();
        if ((v & mask) != 0) {
            return false;
        }
        Config.COMMON.oneTimeEvents.set(v | mask);
        Config.COMMON.oneTimeEvents.save();
        return true;
    }

    /**
     * Find emoji code by given keyword
     *
     * @param keyword keyword
     * @return emoji code collection
     */
    public static List<Pair<String, Integer>> findEmoji(String keyword) {
        return EMOJI_MAP.object2IntEntrySet().stream().filter(e -> e.getKey().contains(keyword)).map(e -> Pair.of(e.getKey(), e.getIntValue())).collect(Collectors.toList());
    }

    public synchronized static void addToEmojiHistory(Pair<String, Integer> emoji) {
        EMOJI_HISTORY.removeIf(e -> e.equals(emoji));
        EMOJI_HISTORY.add(0, emoji);
        if (EMOJI_HISTORY.size() > 15) {
            EMOJI_HISTORY.remove(15);
        }
    }

    public synchronized static List<Pair<String, Integer>> getEmojiHistory() {
        return EMOJI_HISTORY;
    }

    //TODO use json to load map
    static void gEmojiPair() {
        EMOJI_MAP.put("beaming_face_with_smiling_eyes", 0x0000);
        EMOJI_MAP.put("face_with_tears_of_joy", 0x0100);
        EMOJI_MAP.put("grinning_face_with_big_eyes", 0x0200);
        EMOJI_MAP.put("grinning_face_with_happy_eyes", 0x0300);
        EMOJI_MAP.put("grinning_face_with_sweat", 0x0400);
        EMOJI_MAP.put("angry_face_with_horns", 0x0500);
        EMOJI_MAP.put("smiling_face", 0x0600);
        EMOJI_MAP.put("face_savoring_food", 0x0700);
        EMOJI_MAP.put("winking_face", 0x0800);
        EMOJI_MAP.put("smiling_face_with_smiling_eyes", 0x0900);
        EMOJI_MAP.put("relieved_face", 0x0a00);
        EMOJI_MAP.put("smiling_face_with_heart_eyes", 0x0b00);
        EMOJI_MAP.put("smirking_face", 0x0c00);
        EMOJI_MAP.put("confounded_face", 0x0d00);
        EMOJI_MAP.put("unamused_face", 0x0e00);
        EMOJI_MAP.put("downcast_face_with_sweat", 0x0f00);
        EMOJI_MAP.put("pensive_face", 0x1000);
        EMOJI_MAP.put("face_blowing_a_kiss", 0x1100);
        EMOJI_MAP.put("kissing_face_with_closed_eyes", 0x1200);
        EMOJI_MAP.put("winking_face_with_tongue", 0x1300);
        EMOJI_MAP.put("disappointed_face", 0x1400);
        EMOJI_MAP.put("angry_face", 0x1500);
        EMOJI_MAP.put("pouting_face", 0x0001);
        EMOJI_MAP.put("squinting_face_with_tongue", 0x0101);
        EMOJI_MAP.put("crying_face", 0x0201);
        EMOJI_MAP.put("persevering_face", 0x0301);
        EMOJI_MAP.put("face_screaming_in_fear", 0x0401);
        EMOJI_MAP.put("face_with_medical_mask", 0x0501);
        EMOJI_MAP.put("grinning_face_with_smiling_eyes", 0x0601);
        EMOJI_MAP.put("neutral_face", 0x0701);
        EMOJI_MAP.put("expressionless_face", 0x0801);
        EMOJI_MAP.put("sleepy_face", 0x0901);
        EMOJI_MAP.put("smiling_face_with_halo", 0x0a01);
        EMOJI_MAP.put("confused_face", 0x0b01);
        EMOJI_MAP.put("smiling_face_with_sunglasses", 0x0c01);
        EMOJI_MAP.put("kissing_face", 0x0d01);
        EMOJI_MAP.put("sad_but_relieved_face", 0x0e01);
        EMOJI_MAP.put("fearful_face", 0x0f01);
        EMOJI_MAP.put("loudly_crying_face", 0x1001);
        EMOJI_MAP.put("anxious_face_with_sweat", 0x1101);
        EMOJI_MAP.put("face_with_cross_eyes", 0x1201);
        EMOJI_MAP.put("flushed_face", 0x1301);
        EMOJI_MAP.put("grinning_squinting_face", 0x1401);
        EMOJI_MAP.put("kissing_face_with_smiling_eyes", 0x1501);
        EMOJI_MAP.put("people_with_bunny_ears", 0x0002);
        EMOJI_MAP.put("boy", 0x0102);
        EMOJI_MAP.put("child", 0x0202);
        EMOJI_MAP.put("girl", 0x0302);
        EMOJI_MAP.put("man", 0x0402);
        EMOJI_MAP.put("woman", 0x0502);
        EMOJI_MAP.put("woman_and_man_holding_hands", 0x0602);
        EMOJI_MAP.put("person", 0x0702);
        EMOJI_MAP.put("face_with_tongue", 0x0802);
        EMOJI_MAP.put("worried_face", 0x0902);
        EMOJI_MAP.put("face_with_steam_from_nose", 0x0a02);
        EMOJI_MAP.put("frowning_face_with_open_mouth", 0x0b02);
        EMOJI_MAP.put("hushed_face", 0x0c02);
        EMOJI_MAP.put("sleeping_face", 0x0d02);
        EMOJI_MAP.put("mouth", 0x0e02);
        EMOJI_MAP.put("kiss_mark", 0x0f02);
        EMOJI_MAP.put("thumbs_up", 0x1002);
        EMOJI_MAP.put("thumbs_down", 0x1102);
        EMOJI_MAP.put("flexed_biceps", 0x1202);
        EMOJI_MAP.put("open_hands", 0x1302);
        EMOJI_MAP.put("face_with_open_mouth", 0x1402);
        EMOJI_MAP.put("dizzy_face", 0x1502);
        EMOJI_MAP.put("tired_face", 0x0003);
        EMOJI_MAP.put("clapping_hands", 0x0103);
        EMOJI_MAP.put("ear", 0x0203);
        EMOJI_MAP.put("nose", 0x0303);
        EMOJI_MAP.put("nail_polish", 0x0403);
        EMOJI_MAP.put("backhand_index_pointing_down", 0x0503);
        EMOJI_MAP.put("backhand_index_pointing_left", 0x0603);
        EMOJI_MAP.put("folded_hands", 0x0703);
        EMOJI_MAP.put("seedling", 0x0803);
        EMOJI_MAP.put("weary_face", 0x0903);
        EMOJI_MAP.put("grimacing_face", 0x0a03);
        EMOJI_MAP.put("rising_hand", 0x0b03);
        EMOJI_MAP.put("backhand_index_pointing_right", 0x0c03);
        EMOJI_MAP.put("ok_hand", 0x0d03);
        EMOJI_MAP.put("oncoming_fist", 0x0e03);
        EMOJI_MAP.put("anguishes_face", 0x0f03);
        EMOJI_MAP.put("face_without_mouth", 0x1003);
        EMOJI_MAP.put("eyes", 0x1103);
        EMOJI_MAP.put("waving_hand", 0x1203);
        EMOJI_MAP.put("backhand_index_pointing_up", 0x1303);
        EMOJI_MAP.put("victory_hand", 0x1403);
        EMOJI_MAP.put("raised_fist", 0x1503);
        EMOJI_MAP.put("baby_angel", 0x0004);
        EMOJI_MAP.put("santa_claus", 0x0104);
        EMOJI_MAP.put("ghost", 0x0204);
        EMOJI_MAP.put("pile_of_poo", 0x0304);
        EMOJI_MAP.put("skull", 0x0404);
        EMOJI_MAP.put("alien", 0x0504);
        EMOJI_MAP.put("alien_monster", 0x0604);
        EMOJI_MAP.put("palm_tree", 0x0704);
        EMOJI_MAP.put("cactus", 0x0804);
        EMOJI_MAP.put("tulip", 0x0904);
        EMOJI_MAP.put("sunflower", 0x0a04);
        EMOJI_MAP.put("fallen_leaf", 0x0b04);
        EMOJI_MAP.put("leaf_fluttering_in_wind", 0x0c04);
        EMOJI_MAP.put("desktop_computer", 0x0d04);
        EMOJI_MAP.put("television", 0x0e04);
        EMOJI_MAP.put("radio", 0x0f04);
        EMOJI_MAP.put("floppy_disk", 0x1004);
        EMOJI_MAP.put("optical_disk", 0x1104);
        EMOJI_MAP.put("light_bulb", 0x1204);
        EMOJI_MAP.put("satellite_antenna", 0x1304);
        EMOJI_MAP.put("closed_umbrella", 0x1404);
        EMOJI_MAP.put("open_mailbox_with_raised_flag", 0x1504);
        EMOJI_MAP.put("electric_plug", 0x0005);
        EMOJI_MAP.put("watch", 0x0105);
        EMOJI_MAP.put("scissors", 0x0205);
        EMOJI_MAP.put("syringe", 0x0305);
        EMOJI_MAP.put("cherry_blossom", 0x0405);
        EMOJI_MAP.put("rose", 0x0505);
        EMOJI_MAP.put("video_camera", 0x0605);
        EMOJI_MAP.put("printer", 0x0705);
        EMOJI_MAP.put("battery", 0x0805);
        EMOJI_MAP.put("bouquet", 0x0905);
        EMOJI_MAP.put("sheaf_of_rice", 0x0a05);
        EMOJI_MAP.put("MAPle_leaf", 0x0b05);
        EMOJI_MAP.put("camera", 0x0c05);
        EMOJI_MAP.put("briefcase", 0x0d05);
        EMOJI_MAP.put("pencil", 0x0e05);
        EMOJI_MAP.put("pill", 0x0f05);
        EMOJI_MAP.put("hibiscus", 0x1005);
        EMOJI_MAP.put("money_bag", 0x1105);
        EMOJI_MAP.put("credit_card", 0x1205);
        EMOJI_MAP.put("telephone", 0x1305);
        EMOJI_MAP.put("hourglass_done", 0x1405);
        EMOJI_MAP.put("envelope", 0x1505);
        EMOJI_MAP.put("bookmark", 0x0006);
        EMOJI_MAP.put("hundred_point", 0x0106);
        EMOJI_MAP.put("telescope", 0x0206);
        EMOJI_MAP.put("page_facing_up", 0x0306);
        EMOJI_MAP.put("email", 0x0406);
        EMOJI_MAP.put("open_book", 0x0506);
        EMOJI_MAP.put("ledger", 0x0606);
        EMOJI_MAP.put("bookmark_tabs", 0x0706);
        EMOJI_MAP.put("books", 0x0806);
        EMOJI_MAP.put("triangular_ruler", 0x0906);
        EMOJI_MAP.put("straight_ruler", 0x0a06);
        EMOJI_MAP.put("clipboard", 0x0b06);
        EMOJI_MAP.put("bell", 0x0c06);
        EMOJI_MAP.put("bell_with_slash", 0x0d06);
        EMOJI_MAP.put("scroll", 0x0e06);
        EMOJI_MAP.put("atm_sigh", 0x0f06);
        EMOJI_MAP.put("hospital", 0x1006);
        EMOJI_MAP.put("hotel", 0x1106);
        EMOJI_MAP.put("bank", 0x1206);
        EMOJI_MAP.put("japanese_post_office", 0x1306);
        EMOJI_MAP.put("love_hotel", 0x1406);
        EMOJI_MAP.put("wedding", 0x1506);
        EMOJI_MAP.put("school", 0x0007);
        EMOJI_MAP.put("convenience_store", 0x0107);
        EMOJI_MAP.put("department_store", 0x0207);
        EMOJI_MAP.put("wrench", 0x0307);
        EMOJI_MAP.put("hammer", 0x0407);
        EMOJI_MAP.put("microscope", 0x0507);
        EMOJI_MAP.put("office_building", 0x0607);
        EMOJI_MAP.put("japanese_castle", 0x0707);
        EMOJI_MAP.put("unlocked", 0x0807);
        EMOJI_MAP.put("pushpin", 0x0907);
        EMOJI_MAP.put("paperclip", 0x0a07);
        EMOJI_MAP.put("nut_and_bolt", 0x0b07);
        EMOJI_MAP.put("kitchen_knife", 0x0c07);
        EMOJI_MAP.put("old_key", 0x0d07);
        EMOJI_MAP.put("flashlight", 0x0e07);
        EMOJI_MAP.put("pistol", 0x0f07);
        EMOJI_MAP.put("factory", 0x1007);
        EMOJI_MAP.put("castle", 0x1107);
        EMOJI_MAP.put("house", 0x1207);
        EMOJI_MAP.put("statue_of_liberty", 0x1307);
        EMOJI_MAP.put("locked", 0x1407);
        EMOJI_MAP.put("rocket", 0x1507);
        EMOJI_MAP.put("ribbon", 0x0008);
        EMOJI_MAP.put("present", 0x0108);
        EMOJI_MAP.put("firework", 0x0208);
        EMOJI_MAP.put("sparkler", 0x0308);
        EMOJI_MAP.put("graduation_cap", 0x0408);
        EMOJI_MAP.put("crown", 0x0508);
        EMOJI_MAP.put("christmas_tree", 0x0608);
        EMOJI_MAP.put("party_popper", 0x0708);
        EMOJI_MAP.put("carp_streamer", 0x0808);
        EMOJI_MAP.put("birthday_cake", 0x0908);
        EMOJI_MAP.put("jack_o_lantern", 0x0a08);
        EMOJI_MAP.put("japanese_dolls", 0x0b08);
        EMOJI_MAP.put("balloon", 0x0c08);
        EMOJI_MAP.put("dizzy", 0x0d08);
        EMOJI_MAP.put("confetti_ball", 0x0e08);
        EMOJI_MAP.put("shower", 0x0f08);
        EMOJI_MAP.put("bomb", 0x1008);
        EMOJI_MAP.put("seat", 0x1108);
        EMOJI_MAP.put("ship", 0x1208);
        EMOJI_MAP.put("monorail", 0x1308);
        EMOJI_MAP.put("metro", 0x1408);
        EMOJI_MAP.put("fuel_pump", 0x1508);
        EMOJI_MAP.put("construction", 0x0009);
        EMOJI_MAP.put("police_car", 0x0109);
        EMOJI_MAP.put("delivery_truck", 0x0209);
        EMOJI_MAP.put("articulated_lorry", 0x0309);
        EMOJI_MAP.put("tractor", 0x0409);
        EMOJI_MAP.put("bathtub", 0x0509);
        EMOJI_MAP.put("speedboat", 0x0609);
        EMOJI_MAP.put("high_speed_train", 0x0709);
        EMOJI_MAP.put("ambulance", 0x0809);
        EMOJI_MAP.put("fire_engine", 0x0909);
        EMOJI_MAP.put("taxi", 0x0a09);
        EMOJI_MAP.put("sport_utility_vehicle", 0x0b09);
        EMOJI_MAP.put("toilet", 0x0c09);
        EMOJI_MAP.put("airplane", 0x0d09);
        EMOJI_MAP.put("bus", 0x0e09);
        EMOJI_MAP.put("automobile", 0x0f09);
        EMOJI_MAP.put("trolleybus", 0x1009);
        EMOJI_MAP.put("bicycle", 0x1109);
        EMOJI_MAP.put("locomotive", 0x1209);
        EMOJI_MAP.put("helicopter", 0x1309);
        EMOJI_MAP.put("horizontal_traffic_light", 0x1409);
        EMOJI_MAP.put("mountain_cableway", 0x1509);
        EMOJI_MAP.put("moon_viewing_ceremony", 0x000a);
        EMOJI_MAP.put("ring", 0x010a);
        EMOJI_MAP.put("gem_stone", 0x020a);
        EMOJI_MAP.put("heart_with_arrow", 0x030a);
        EMOJI_MAP.put("red_paper_lantern", 0x040a);
        EMOJI_MAP.put("woman’s_hat", 0x050a);
        EMOJI_MAP.put("dress", 0x060a);
        EMOJI_MAP.put("running_shoe", 0x070a);
        EMOJI_MAP.put("wind_chime", 0x080a);
        EMOJI_MAP.put("love_letter", 0x090a);
        EMOJI_MAP.put("jeans", 0x0a0a);
        EMOJI_MAP.put("red_heart", 0x0b0a);
        EMOJI_MAP.put("broken_heart", 0x0c0a);
        EMOJI_MAP.put("heart_with_ribbon", 0x0d0a);
        EMOJI_MAP.put("glasses", 0x0e0a);
        EMOJI_MAP.put("t_shirt", 0x0f0a);
        EMOJI_MAP.put("bikini", 0x100a);
        EMOJI_MAP.put("pine_decoration", 0x110a);
        EMOJI_MAP.put("necktie", 0x120a);
        EMOJI_MAP.put("handbag", 0x130a);
        EMOJI_MAP.put("man's_shoe", 0x140a);
        EMOJI_MAP.put("high_heeled_shoe", 0x150a);
        EMOJI_MAP.put("footprints", 0x000b);
        EMOJI_MAP.put("backpack", 0x010b);
        EMOJI_MAP.put("lipstick", 0x020b);
        EMOJI_MAP.put("person_running", 0x030b);
        EMOJI_MAP.put("snowboarder", 0x040b);
        EMOJI_MAP.put("person_surfing", 0x050b);
        EMOJI_MAP.put("person_rowing_boat", 0x060b);
        EMOJI_MAP.put("person_swimming", 0x070b);
        EMOJI_MAP.put("horse_riding", 0x080b);
        EMOJI_MAP.put("woman_dancing", 0x090b);
        EMOJI_MAP.put("joker", 0x0a0b);
        EMOJI_MAP.put("soccer_ball", 0x0b0b);
        EMOJI_MAP.put("top_hat", 0x0c0b);
        EMOJI_MAP.put("baseball", 0x0d0b);
        EMOJI_MAP.put("tent", 0x0e0b);
        EMOJI_MAP.put("flag_in_hole", 0x0f0b);
        EMOJI_MAP.put("tennis", 0x100b);
        EMOJI_MAP.put("american_football", 0x110b);
        EMOJI_MAP.put("pool_8_ball", 0x120b);
        EMOJI_MAP.put("basketball", 0x130b);
        EMOJI_MAP.put("bowling", 0x140b);
        EMOJI_MAP.put("artist_palette", 0x150b);
        EMOJI_MAP.put("circus_tent", 0x000c);
        EMOJI_MAP.put("direct_hit", 0x010c);
        EMOJI_MAP.put("slot_machine", 0x020c);
        EMOJI_MAP.put("game_die", 0x030c);
        EMOJI_MAP.put("sailboat", 0x040c);
        EMOJI_MAP.put("microphone", 0x050c);
        EMOJI_MAP.put("headphone", 0x060c);
        EMOJI_MAP.put("saxophone", 0x070c);
        EMOJI_MAP.put("musical_note", 0x080c);
        EMOJI_MAP.put("musical_notes", 0x090c);
        EMOJI_MAP.put("movie_camera", 0x0a0c);
        EMOJI_MAP.put("violin", 0x0b0c);
        EMOJI_MAP.put("musical_keyboard", 0x0c0c);
        EMOJI_MAP.put("trumpet", 0x0d0c);
        EMOJI_MAP.put("aries", 0x0e0c);
        EMOJI_MAP.put("taurus", 0x0f0c);
        EMOJI_MAP.put("gemini", 0x100c);
        EMOJI_MAP.put("musical_score", 0x110c);
        EMOJI_MAP.put("scorpio", 0x120c);
        EMOJI_MAP.put("sagittarius", 0x130c);
        EMOJI_MAP.put("men's_room", 0x140c);
        EMOJI_MAP.put("baby_symbol", 0x150c);
        EMOJI_MAP.put("no_smoking", 0x000d);
        EMOJI_MAP.put("ophiuchus", 0x010d);
        EMOJI_MAP.put("tiger_face", 0x020d);
        EMOJI_MAP.put("trophy", 0x030d);
        EMOJI_MAP.put("skis", 0x040d);
        EMOJI_MAP.put("aquarius", 0x050d);
        EMOJI_MAP.put("mouse_face", 0x060d);
        EMOJI_MAP.put("hamster", 0x070d);
        EMOJI_MAP.put("cow_face", 0x080d);
        EMOJI_MAP.put("guitar", 0x090d);
        EMOJI_MAP.put("capricorn", 0x0a0d);
        EMOJI_MAP.put("pisces", 0x0b0d);
        EMOJI_MAP.put("restroom", 0x0c0d);
        EMOJI_MAP.put("video_game", 0x0d0d);
        EMOJI_MAP.put("ticket", 0x0e0d);
        EMOJI_MAP.put("clapper_board", 0x0f0d);
        EMOJI_MAP.put("chequered_flag", 0x100d);
        EMOJI_MAP.put("cancer", 0x110d);
        EMOJI_MAP.put("leo", 0x120d);
        EMOJI_MAP.put("virgo", 0x130d);
        EMOJI_MAP.put("libra", 0x140d);
        EMOJI_MAP.put("women's_room", 0x150d);
        EMOJI_MAP.put("horse", 0x000e);
        EMOJI_MAP.put("ewe", 0x010e);
        EMOJI_MAP.put("penguin", 0x020e);
        EMOJI_MAP.put("elephant", 0x030e);
        EMOJI_MAP.put("camel", 0x040e);
        EMOJI_MAP.put("boar", 0x050e);
        EMOJI_MAP.put("rabbit_face", 0x060e);
        EMOJI_MAP.put("cat_face", 0x070e);
        EMOJI_MAP.put("racing_horse", 0x080e);
        EMOJI_MAP.put("chicken", 0x090e);
        EMOJI_MAP.put("baby_chick", 0x0a0e);
        EMOJI_MAP.put("bird", 0x0b0e);
        EMOJI_MAP.put("pig_face", 0x0c0e);
        EMOJI_MAP.put("dog_face", 0x0d0e);
        EMOJI_MAP.put("wolf", 0x0e0e);
        EMOJI_MAP.put("bear", 0x0f0e);
        EMOJI_MAP.put("koala", 0x100e);
        EMOJI_MAP.put("monkey_face", 0x110e);
        EMOJI_MAP.put("monkey", 0x120e);
        EMOJI_MAP.put("snake", 0x130e);
        EMOJI_MAP.put("frog", 0x140e);
        EMOJI_MAP.put("spouting_whale", 0x150e);
        EMOJI_MAP.put("dolphin", 0x000f);
        EMOJI_MAP.put("octopus", 0x010f);
        EMOJI_MAP.put("tropical_fish", 0x020f);
        EMOJI_MAP.put("spiral_shell", 0x030f);
        EMOJI_MAP.put("bug", 0x040f);
        EMOJI_MAP.put("honeybee", 0x050f);
        EMOJI_MAP.put("lady_beetle", 0x060f);
        EMOJI_MAP.put("panda", 0x070f);
        EMOJI_MAP.put("pig_nose", 0x080f);
        EMOJI_MAP.put("high_voltage", 0x090f);
        EMOJI_MAP.put("sun", 0x0a0f);
        EMOJI_MAP.put("paw_prints", 0x0b0f);
        EMOJI_MAP.put("turtle", 0x0c0f);
        EMOJI_MAP.put("crescent_moon", 0x0d0f);
        EMOJI_MAP.put("ant", 0x0e0f);
        EMOJI_MAP.put("dragon_face", 0x0f0f);
        EMOJI_MAP.put("tongue", 0x100f);
        EMOJI_MAP.put("fire", 0x110f);
        EMOJI_MAP.put("fish", 0x120f);
        EMOJI_MAP.put("snail", 0x130f);
        EMOJI_MAP.put("hatching_chick", 0x140f);
        EMOJI_MAP.put("front_facing_baby_chick", 0x150f);
        EMOJI_MAP.put("cloud", 0x0010);
        EMOJI_MAP.put("dashing_away", 0x0110);
        EMOJI_MAP.put("snowflake", 0x0210);
        EMOJI_MAP.put("water_wave", 0x0310);
        EMOJI_MAP.put("ear_of_corn", 0x0410);
        EMOJI_MAP.put("herb", 0x0510);
        EMOJI_MAP.put("four_leaf_clover", 0x0610);
        EMOJI_MAP.put("melon", 0x0710);
        EMOJI_MAP.put("sweat_droplets", 0x0810);
        EMOJI_MAP.put("umbrella_with_rain_drops", 0x0910);
        EMOJI_MAP.put("star", 0x0a10);
        EMOJI_MAP.put("sunrise", 0x0b10);
        EMOJI_MAP.put("globe_showing_europe_africa", 0x0c10);
        EMOJI_MAP.put("mushroom", 0x0d10);
        EMOJI_MAP.put("eggplant", 0x0e10);
        EMOJI_MAP.put("grapes", 0x0f10);
        EMOJI_MAP.put("sunrise_over_mountains_", 0x1010);
        EMOJI_MAP.put("blossom", 0x1110);
        EMOJI_MAP.put("tomato", 0x1210);
        EMOJI_MAP.put("rainbow", 0x1310);
        EMOJI_MAP.put("chestnut", 0x1410);
        EMOJI_MAP.put("evergreen_tree", 0x1510);
        EMOJI_MAP.put("deciduous_tree", 0x0011);
        EMOJI_MAP.put("watermelon", 0x0111);
        EMOJI_MAP.put("tangerine", 0x0211);
        EMOJI_MAP.put("lemon", 0x0311);
        EMOJI_MAP.put("banana", 0x0411);
        EMOJI_MAP.put("pineapple", 0x0511);
        EMOJI_MAP.put("red_apple", 0x0611);
        EMOJI_MAP.put("cherries", 0x0711);
        EMOJI_MAP.put("green_apple", 0x0811);
        EMOJI_MAP.put("pear", 0x0911);
        EMOJI_MAP.put("peach", 0x0a11);
        EMOJI_MAP.put("strawberry", 0x0b11);
        EMOJI_MAP.put("hamburger", 0x0c11);
        EMOJI_MAP.put("rice_cracker", 0x0d11);
        EMOJI_MAP.put("rice_ball", 0x0e11);
        EMOJI_MAP.put("cooked_rice", 0x0f11);
        EMOJI_MAP.put("steaming_bowl", 0x1011);
        EMOJI_MAP.put("curry_rice", 0x1111);
        EMOJI_MAP.put("spaghetti", 0x1211);
        EMOJI_MAP.put("bread", 0x1311);
        EMOJI_MAP.put("french_fries", 0x1411);
        EMOJI_MAP.put("dango", 0x1511);
        EMOJI_MAP.put("oden", 0x0012);
        EMOJI_MAP.put("soft_ice_cream", 0x0112);
        EMOJI_MAP.put("shaved_ice", 0x0212);
        EMOJI_MAP.put("sushi", 0x0312);
        EMOJI_MAP.put("pizza", 0x0412);
        EMOJI_MAP.put("meat_on_bone", 0x0512);
        EMOJI_MAP.put("poultry_leg", 0x0612);
        EMOJI_MAP.put("cookie", 0x0712);
        EMOJI_MAP.put("roasted_sweet_potato", 0x0812);
        EMOJI_MAP.put("fried_shrimp", 0x0912);
        EMOJI_MAP.put("doughnut", 0x0a12);
        EMOJI_MAP.put("chocolate_bar", 0x0b12);
        EMOJI_MAP.put("bento_box", 0x0c12);
        EMOJI_MAP.put("pot_of_food", 0x0d12);
        EMOJI_MAP.put("wine_glass", 0x0e12);
        EMOJI_MAP.put("beer_mug", 0x0f12);
        EMOJI_MAP.put("clinking_beer_mugs", 0x1012);
        EMOJI_MAP.put("custard", 0x1112);
        EMOJI_MAP.put("fork_and_knife", 0x1212);
        EMOJI_MAP.put("candy", 0x1312);
        EMOJI_MAP.put("tropical_drink", 0x1412);
        EMOJI_MAP.put("snowman", 0x1512);
        EMOJI_MAP.put("teacup_without_handle", 0x0013);
        EMOJI_MAP.put("hot_beverage", 0x0113);
        EMOJI_MAP.put("sake", 0x0213);
        EMOJI_MAP.put("baby_bottle", 0x0313);
        EMOJI_MAP.put("cocktail_glass", 0x0413);
        EMOJI_MAP.put("shortcake", 0x0513);
        EMOJI_MAP.put("cooking", 0x0613);
        EMOJI_MAP.put("lollipop", 0x0713);
        EMOJI_MAP.put("honey_pot", 0x0813);
        EMOJI_MAP.put("sun_behind_cloud", 0x0913);
        EMOJI_MAP.put("mezz", 0x0a13);
        EMOJI_MAP.put("snownee", 0x0b13);
        EMOJI_MAP.put("tenma_gabriel_white", 0x0c13);
    }
}
