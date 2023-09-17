#include <algorithm>
#include <cstring>
#include <iostream>
#include <set>
#include <vector>

struct Span {
    explicit Span(int s, int e = -1, char c = 0)
            : st(s), en(e), ch(c) {}

    bool operator<(const Span& rhs) const {
        return st < rhs.st;
    }

    int st, en;
    char ch;
};

std::set<Span> tree;

inline auto split(int x, int n) {
    if (x > n)
        return tree.end();
    auto it = --tree.upper_bound(Span(x));
    if (it->st == x)
        return it;
    int s = it->st, e = it->en;
    char c = it->ch;
    tree.erase(it);
    tree.emplace(s, x - 1, c);
    return tree.emplace(x, e, c).first;
}

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    int n, q, l, r, k;
    std::string str;
    std::cin >> n >> q;
    std::cin >> str;
    for (int i = 1; i <= n; ++i)
        tree.emplace(i, i, str[i - 1]);
    std::vector<int> cnt(26);
    while (q--) {
        std::cin >> l >> r >> k;
        auto en = split(r + 1, n), st = split(l, n);
        memset(&cnt[0], 0, 26 * sizeof cnt[0]);
        for (auto e = st; e != en; ++e)
            cnt[e->ch - 'a'] += e->en - e->st + 1;
        auto f = [&](int i) {
            if (cnt[i]) tree.emplace(l, l + cnt[i] - 1, i + 'a'), l += cnt[i];
        };
        tree.erase(st, en);
        if (k) for (int i = 0; i <= 25; ++i) f(i);
        else for (int i = 25; i >= 0; --i) f(i);
    }
    for (auto e : tree)
        for (int i = e.en - e.st; i >= 0; --i)
            std::cout << e.ch;
    return 0;
}