/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.test;

import icyllis.modernui.util.BinaryIO;
import icyllis.modernui.util.DataSet;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.*;

public class TestDataSet {

    public static void main(String[] args) {
        Configurator.setRootLevel(Level.ALL);
        write();
        read();
    }

    static void write() {
        DataSet resp = new DataSet();
        resp.put("health", 5);
        resp.put("velocity", 9.2f);
        DataSet inner = new DataSet();
        inner.put("x", 6.1f);
        inner.put("y", 56.2f);
        resp.put("pos", inner);
        try {
            BinaryIO.deflate(new FileOutputStream("resp.dat"), resp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void read() {
        DataSet resp;
        try {
            resp = BinaryIO.inflate(new FileInputStream("resp.dat"), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println(resp); // {health=5, velocity=9.2, pos={x=6.1, y=56.2}}
    }
}
