#include <bits/stdc++.h>

// sum [1,n] phi(n), sum of Euler's totient function
int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    int in;
    std::cin >> in;
    const int n = std::pow(in, 2.0 / 3);
    std::vector<int64_t> sum(n + 1);
    sum[1] = 1;
    for (int i = 2; i <= n; i++) {
        if (sum[i]) continue;
        for (int j = i; j <= n; j += i) {
            if (sum[j] == 0) sum[j] = j;
            sum[j] = sum[j] / i * (i - 1);
        }
    }
    for (int i = 1; i <= n; i++)
        sum[i] += sum[i - 1];
    std::unordered_map<int64_t, int64_t> map;
    auto f = [&](auto&& self, int64_t x) {
        if (x <= n) return sum[x];
        auto& v = map[x];
        if (v) return v;
        auto res = x * (x + 1) / 2;
        for (int64_t i = 2, j; i <= x; i = j + 1) {
            j = x / (x / i);
            res -= self(self, x / i) * (j - i + 1);
        }
        return v = res;
    };
    auto a = f(f, in) - in, b = (int64_t) (in - 1) * (in - 2) / 2, gcd = std::gcd(a, b);
    std::cout << (a / gcd) << '/' << (b / gcd) << '\n';
    return 0;
}