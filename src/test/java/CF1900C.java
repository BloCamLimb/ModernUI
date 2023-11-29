/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

import java.io.*;
import java.util.StringTokenizer;

public class CF1900C {

    public static void main(String[] args) {
        var sc = new Scanner(System.in);
        var pw = new PrintWriter(System.out);
        int t = sc.nextInt();
        while (t-- != 0) {
            int n = sc.nextInt();
            TreeNode[] tree = new TreeNode[n + 1];
            String marks = sc.next();
            for (int i = 1; i <= n; i++) {
                tree[i] = new TreeNode(sc.nextInt(), sc.nextInt(), marks.charAt(i - 1));
            }
            pw.println(findDepth(tree, 1));
        }
        pw.flush();
    }

    public static int findDepth(TreeNode[] tree, int cur) {
        if (cur == 0) return 0;
        TreeNode node = tree[cur];
        int leftDepth = findDepth(tree, node.left);
        int rightDepth = findDepth(tree, node.right);
        if (node.left == 0 && node.right != 0) {
            return rightDepth + (node.mark == 'R' ? 0 : 1);
        }
        if (node.left != 0 && node.right == 0) {
            return leftDepth + (node.mark == 'L' ? 0 : 1);
        }
        if (node.left == 0) {
            return 0;
        }
        return Math.min(
                leftDepth + (node.mark == 'L' ? 0 : 1),
                rightDepth + (node.mark == 'R' ? 0 : 1)
        );
    }

    public record TreeNode(int left, int right, char mark) {
    }

    static class Scanner {

        final BufferedReader br;
        StringTokenizer st;

        Scanner(InputStream in) {
            br = new BufferedReader(new InputStreamReader(in));
        }

        String next() {
            while (st == null || !st.hasMoreElements()) {
                try {
                    st = new StringTokenizer(br.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return st.nextToken();
        }

        int nextInt() {
            return Integer.parseInt(next());
        }
    }
}
