#include <cstring>
#include <iostream>

int dp[1001];

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    int m, n, x;
    while (std::cin >> m >> n) {
        memset(dp, 0, std::max(m, n) * sizeof(int));
        while (m--) for (int i = 1; i <= n; i++) {
            std::cin >> x;
            dp[i] = std::max(dp[i], dp[i - 1]) + x;
        }
        std::cout << dp[n] << std::endl;
    }
    return 0;
}