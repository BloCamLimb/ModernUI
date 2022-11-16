#include <iostream>

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    // Kadane’s Algorithm
    int t, n, v, cur, max, p, st, en;
    std::cin >> t;
    while (t--) {
        std::cin >> n >> v;
        cur = max = v;
        p = st = en = 0;
        for (int i = 1; i < n; i++) {
            std::cin >> v;
            if (cur < 0) {
                cur = v;
                p = i;
            } else {
                cur += v;
            }
            if (cur > max) {
                max = cur;
                st = p;
                en = i;
            }
        }
        std::cout << max << ' ' << st + 1 << ' ' << en + 1 << std::endl;
    }
    return 0;
}