#include <cstring>
#include <iostream>

constexpr int N = 100;
int dp[N + 1][N + 1];
char s[N + 1][N + 1];
char a[N + 1], b[N + 1];

void lcs(const char* x, const char* y, int m, int n) {
    memset(dp, 0, sizeof(dp));
    for (int i = 1; i <= m; i++) {
        for (int j = 1; j <= n; j++) {
            if (x[i - 1] == y[j - 1])
                dp[i][j] = dp[i - 1][j - 1] + 1, s[i][j] = 0;
            else if (dp[i - 1][j] >= dp[i][j - 1])
                dp[i][j] = dp[i - 1][j], s[i][j] = 1;
            else
                dp[i][j] = dp[i][j - 1], s[i][j] = 2;
        }
    }
}

void out(int i, int j) {
    if (i == 0 || j == 0) {
        return;
    }
    switch (s[i][j]) {
        case 0:
            out(i - 1, j - 1);
            std::cout << a[i - 1];
            break;
        case 1:
            out(i - 1, j);
            break;
        case 2:
            out(i, j - 1);
    }
}

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    int m, n;
    while (std::cin >> a >> b) {
        m = strlen(a), n = strlen(b);
        lcs(a, b, m, n);
        out(m, n);
        std::cout << std::endl;
    }
    return 0;
}