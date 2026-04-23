# Deployment Guide — Book Catalogue

> Інструкція для release engineer / DevOps щодо розгортання застосунку у production-середовищі.

## Опис середовища розгортання

Book Catalogue — десктопний Java-застосунок. «Production» означає розповсюдження
виконуваного JAR-файлу кінцевим користувачам або встановлення на корпоративних робочих
станціях. Жодного сервера, бази даних або мережевих сервісів не потрібно.

---

## 1. Вимоги до апаратного забезпечення

| Параметр | Мінімум | Рекомендовано |
|---|---|---|
| Архітектура | x86-64 або ARM64 | x86-64 або Apple Silicon (arm64) |
| CPU | 1 ядро, 1 GHz | 2+ ядра |
| RAM | 256 MB | 512 MB+ |
| Диск | 50 MB вільного місця | 200 MB (для JRE + даних) |
| Дисплей | Будь-який з роздільною здатністю ≥ 800×600 | 1280×720+ |

---

## 2. Необхідне програмне забезпечення

### На машині, де виконується збірка (build server / CI)

| ПЗ | Версія | Призначення |
|---|---|---|
| JDK | 17 LTS або новіше | Компіляція та пакування |
| Git | 2.x | Клонування репозиторію |

### На машині кінцевого користувача

| ПЗ | Версія | Примітка |
|---|---|---|
| JRE або JDK | 17 LTS або новіше | Мінімум — JRE (headless не підійде, потрібен desktop JRE) |

Перевірка:
```bash
java -version
# Очікувано: openjdk version "17.x.x"
```

---

## 3. Налаштування мережі

Застосунок не використовує мережу. Жодних відкритих портів не потрібно.
Відкритий інтернет-доступ потрібен лише при першому завантаженні JRE на машину користувача.

---

## 4. Процес збірки та розгортання

### 4.1. Клонування репозиторію (на build-машині)

```bash
git clone https://github.com/MariaNazarenkoStud/Library.git
cd Library
git checkout main
```

### 4.2. Збірка виконуваного JAR

```bash
chmod +x docs/scripts/build.sh
./docs/scripts/build.sh
```

Скрипт виконує:
1. Компіляцію всіх `.java` файлів у `out/`
2. Пакування у `dist/BookCatalogue.jar` з manifest (Main-Class: BookGUI)
3. Генерацію Javadoc у `docs/javadoc/`

Після виконання:
```
dist/
└── BookCatalogue.jar    ← дистрибутивний файл
```

### 4.3. Розповсюдження

**Варіант A — ручна установка:**

Передати користувачу файл `BookCatalogue.jar`. Запуск:
```bash
java -jar BookCatalogue.jar
```

**Варіант B — пакет із скриптом запуску:**

```bash
mkdir -p release/BookCatalogue
cp dist/BookCatalogue.jar release/BookCatalogue/
cp docs/scripts/run.sh release/BookCatalogue/
chmod +x release/BookCatalogue/run.sh
cd release
zip -r BookCatalogue-v1.0.zip BookCatalogue/
```

Вміст архіву:
```
BookCatalogue/
├── BookCatalogue.jar
└── run.sh
```

**Варіант C — Windows `.bat`:**
```bat
@echo off
java -jar BookCatalogue.jar
pause
```

Зберегти як `run.bat` поруч з JAR-файлом.

### 4.4. Встановлення на цільовій машині

1. Переконатися, що JRE 17+ встановлено (`java -version`)
2. Скопіювати `BookCatalogue.jar` у будь-яку зручну директорію, наприклад:
   - **Linux/macOS:** `~/Applications/BookCatalogue/`
   - **Windows:** `C:\Program Files\BookCatalogue\`
3. Запустити застосунок

---

## 5. Конфігурація середовища

Застосунок не потребує файлів конфігурації. Усі налаштування вбудовані.

Якщо потрібно перевизначити директорію для збереження даних:
```bash
java -Duser.home=/custom/path -jar BookCatalogue.jar
```
Файли `.dat` за замовчуванням зберігаються там, де запускається застосунок.

---

## 6. Перевірка працездатності

### Функціональна перевірка після розгортання:

1. Запустити `java -jar BookCatalogue.jar` — вікно «Book Catalogue» повинно відкритися
2. Додати тестову книгу (Title: Test, Author: Test, Year: 2024)
3. Натиснути «Add book» — книга з'являється в таблиці
4. «Save to file» → зберегти як `test.dat`
5. «Remove book» → видалити тестову книгу
6. «Load from file» → завантажити `test.dat` — книга знову в таблиці
7. Видалити `test.dat`

**Результат: усі 6 кроків пройшли успішно** — розгортання виконано коректно.

### Перевірка версії JAR:

```bash
java -cp dist/BookCatalogue.jar BookGUI --version 2>&1 || \
  unzip -p dist/BookCatalogue.jar META-INF/MANIFEST.MF
```

---

## 7. Відомі обмеження

| Обмеження | Деталі |
|---|---|
| GUI не працює без display | На headless-серверах (без монітора) застосунок не запуститься |
| Серіалізація Java | `.dat`-файли несумісні між різними версіями застосунку, якщо змінилися поля класів |
| Один користувач | Одночасна робота декількох процесів з одним `.dat`-файлом не підтримується |
