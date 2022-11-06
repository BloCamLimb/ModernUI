#include <algorithm>
#include <iostream>

int a[1000], b[1000];

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    int m, n, c, i, j, ans;
    auto cmp = [](int a, int b) {
        return a < b;
    };
    while (std::cin >> m >> n >> c) {
        for (i = 0; i < m; i++)
            std::cin >> a[i];
        for (i = 0; i < n; i++)
            std::cin >> b[i];
        std::sort(a, a + m, cmp);
        std::sort(b, b + n, cmp);
        i = j = ans = 0;
        while (i < m && j < n) {
            if (a[i] - b[j] > c) j++;
            else if (b[j] - a[i] > c) i++;
            else {
                i++;
                j++;
                ans++;
            }
        }
        std::cout << ans << std::endl;
    }
    return 0;
}