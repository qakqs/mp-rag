-- ============================================================
-- 员工表 & 部门表 示例 SQL 查询
-- ============================================================

-- 建表语句（可选）
CREATE TABLE IF NOT EXISTS departments (
    department_id   INT PRIMARY KEY,
    department_name VARCHAR(50) NOT NULL,
    manager_id      INT
);

CREATE TABLE IF NOT EXISTS employees (
    employee_id    INT PRIMARY KEY,
    name           VARCHAR(50) NOT NULL,
    department_id  INT,
    salary         DECIMAL(10, 2),
    hire_date      DATE,
    FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

-- 测试数据
INSERT INTO departments (department_id, department_name, manager_id) VALUES
    (101, '技术部', 1),
    (102, '销售部', 3),
    (103, '人事部', 5);

INSERT INTO employees (employee_id, name, department_id, salary, hire_date) VALUES
    (1, '张三', 101, 8000, '2020-01-15'),
    (2, '李四', 101, 7500, '2019-03-20'),
    (3, '王五', 102, 9000, '2018-07-10'),
    (4, '赵六', 102, 8500, '2021-05-30'),
    (5, '钱七', 103, 7000, '2020-11-25');

-- ============================================================
-- 1. 查询每个部门的部门名称、员工数量、平均薪资
--    LEFT JOIN 确保无员工的部门也展示（员工数和平均薪资为 0/NULL）
-- ============================================================
SELECT
    d.department_name,
    COUNT(e.employee_id) AS employee_count,
    AVG(e.salary)          AS avg_salary
FROM departments d
LEFT JOIN employees e ON d.department_id = e.department_id
GROUP BY d.department_id, d.department_name;

-- ============================================================
-- 2. 查询薪资高于本部门平均薪资的员工信息
--    使用窗口函数，一次扫表，比关联子查询效率高
-- ============================================================
SELECT employee_id, name, department_id, salary, hire_date
FROM (
    SELECT *,
           AVG(salary) OVER (PARTITION BY department_id) AS dept_avg_salary
    FROM employees
) t
WHERE salary > dept_avg_salary;

-- 预期结果：
-- | 1 | 张三 | 101 | 8000 | 2020-01-15 |
-- | 3 | 王五 | 102 | 9000 | 2018-07-10 |

-- ============================================================
-- 3. 查询没有员工的部门信息
--    NOT EXISTS 语义优于 LEFT JOIN + IS NULL
-- ============================================================
SELECT *
FROM departments d
WHERE NOT EXISTS (
    SELECT 1
    FROM employees e
    WHERE e.department_id = d.department_id
);

-- 当前数据所有部门均有员工，结果为空。
-- 插入测试部门验证：INSERT INTO departments VALUES (104, '行政部', NULL);
