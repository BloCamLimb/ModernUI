#include <iostream>

constexpr int N = 100;
int parent[N + 1], rank[N + 1];

int find(int i) {
    return parent[i] != i ? parent[i] = find(parent[i]) : i;
}

void combine(int x, int y) {
    x = find(x);
    y = find(y);
    if (x == y) return;
    int rx = rank[x], ry = rank[y];
    if (rx > ry) parent[y] = x;
    else {
        parent[x] = y;
        if (rx == ry) rank[y]++;
    }
}

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    int n, m, x, y;
    std::cin >> n >> m;
    for (int i = 1; i <= n; i++) {
        parent[i] = i;
        rank[i] = 1;
    }
    while (m--) {
        std::cin >> x >> y;
        combine(x, y);
    }
    int comp = 0;
    for (int i = 1; comp < 2 && i <= n; i++)
        if (find(i) == i) comp++;
    std::cout << (comp == 1 ? "Yes" : "No") << std::endl;
    return 0;
}