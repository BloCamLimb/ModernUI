#include <bits/stdc++.h>

#define N 100000

class SegTree {
public:
    SegTree() noexcept {
        build(1, 1, N);
    }

    void add(int pos, int where, int change) {
        Node& n = tree[pos];
        if (n.start == where && n.end == where) {
            n.max += change;
        } else {
            int mid = (n.start + n.end) >> 1;
            int lst = pos << 1, rst = lst + 1;
            if (where <= mid)
                add(lst, where, change);
            else
                add(rst, where, change);
            n.max = std::max(tree[lst].max, tree[rst].max);
        }
    }

    int max(int pos) {
        return tree[pos].max;
    }

private:
    struct Node {
        int start = 0, end = 0;
        int max = 0;
    } tree[1 << 18]; // = CeilPow2(N + 1) << 1

    void build(int pos, int start, int end) {
        Node& n = tree[pos];
        n.start = start, n.end = end;
        if (start != end) {
            int mid = (start + end) >> 1;
            build(pos << 1, start, mid);
            build((pos << 1) + 1, mid + 1, end);
        }
    }
};

SegTree st;

void add(int value, int change) {
    for (int i = 2; i * i <= value; i++)
        if (value % i == 0) {
            while (value % i == 0)
                value /= i;
            st.add(1, i, change);
        }
    if (value != 1)
        st.add(1, value, change);
}

int a[N + 1], limit[N + 1];

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    int n, m, i, j = 0;
    std::cin >> n >> m;
    for (i = 1; i <= n; i++)
        std::cin >> a[i];
    for (i = 1; i <= n; i++) {
        while (j + 1 <= n) {
            add(a[j + 1], +1);
            if (st.max(1) > 2) {
                add(a[j + 1], -1);
                break;
            }
            j++;
        }
        limit[i] = j;
        add(a[i], -1);
    }
    for (n = 0; n < m; n++) {
        std::cin >> i >> j;
        std::cout << (j > limit[i] ? "YES\n" : "NO\n");
    }
    return 0;
}