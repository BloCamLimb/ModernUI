#include <algorithm>
#include <iostream>
#include <tuple>
#include <vector>

struct Student {
    Student();
    Student(const Student&);
    Student(Student&&) noexcept;

    Student& operator=(const Student&);
    Student& operator=(Student&&) noexcept;

    bool operator<(const Student& rhs) const;

    std::string fName;
    int fNumber = 0;
    int fScore = 0;
    int fTotal = 0;
};

std::vector<Student> students;

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);
    int n, x, y;
    while (std::cin >> n) {
        for (int i = 0; i < n; i++) {
            Student stu;
            std::cin >> stu.fName;
            std::cin >> stu.fNumber;
            std::cin >> stu.fScore;
            std::cin >> x >> y;
            stu.fTotal = stu.fScore + x + y;
            students.push_back(std::move(stu));
        }
        std::sort(students.begin(), students.end());
        for (int i = 0, e = std::min(n, 5); i < e; i++) {
            const auto& stu = students[i];
            std::cout << stu.fName << " " << stu.fNumber << " " << stu.fTotal << std::endl;
        }
        students.clear();
    }
    return 0;
}

Student::Student() = default;
Student::Student(const Student&) = default;
Student::Student(Student&&) noexcept = default;

Student& Student::operator=(const Student&) = default;
Student& Student::operator=(Student&&) noexcept = default;

bool Student::operator<(const Student& rhs) const {
    return std::tie(rhs.fTotal, rhs.fScore, rhs.fNumber) <
           std::tie(fTotal, fScore, fNumber);
}