/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

import java.util.*;

// RE in OJ, use C++ version
public class LevP1253 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        record Student(String name, int number, int score, int total) {
            Student(String name, int number, int score0, int score1, int score2) {
                this(name, number, score0, score0 + score1 + score2);
            }
        }
        List<Student> students = new ArrayList<>();
        while (scanner.hasNext()) {
            int n = scanner.nextInt();
            for (int i = 0; i < n; i++)
                students.add(new Student(scanner.next(), scanner.nextInt(),
                        scanner.nextInt(), scanner.nextInt(), scanner.nextInt()));
            students.sort(Comparator.comparingInt(Student::total)
                    .thenComparingInt(Student::score)
                    .thenComparingInt(Student::number)
                    .reversed());
            for (int i = 0, e = Math.min(n, 5); i < e; i++) {
                Student stu = students.get(i);
                System.out.printf("%s %d %d\n", stu.name(), stu.number(), stu.total());
            }
            students.clear();
        }
    }
}
