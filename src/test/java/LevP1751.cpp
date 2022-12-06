#include <iostream>

int map[1000001];

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    int n, x, max = 0;
    std::cin >> n;
    for (int i = 1; i <= n; i++) {
        std::cin >> x;
        if (map[x]) max = std::max(max, i - map[x]);
        else map[x] = i;
    }
    std::cout << max << std::endl;
    return 0;
}