#include <algorithm>
#include <iostream>
#include <tuple>
#include <vector>

struct Shop {
    Shop();
    Shop(const Shop&);
    Shop(Shop&&) noexcept;

    Shop& operator=(const Shop&);
    Shop& operator=(Shop&&) noexcept;

    bool operator<(const Shop& rhs) const;

    int fPrice = 0;
    int fStock = 0;
};

std::vector<Shop> shops;

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    int m, n, buy, ans;
    std::cin >> m >> n;
    while (n--) {
        Shop shop;
        std::cin >> shop.fPrice;
        std::cin >> shop.fStock;
        shops.push_back(std::move(shop));
    }
    std::sort(shops.begin(), shops.end());
    auto it = shops.begin();
    for (;;) {
        buy = std::min(m, it->fStock);
        ans += buy * it->fPrice;
        m -= buy;
        if (m) it++;
        else break;
    }
    std::cout << ans << std::endl;
    return 0;
}

Shop::Shop() = default;
Shop::Shop(const Shop&) = default;
Shop::Shop(Shop&&) noexcept = default;

Shop& Shop::operator=(const Shop&) = default;
Shop& Shop::operator=(Shop&&) noexcept = default;

bool Shop::operator<(const Shop& rhs) const {
    return fPrice < rhs.fPrice;
}