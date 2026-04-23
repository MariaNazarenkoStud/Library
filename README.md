# Каталог книг

Десктопний застосунок для управління особистою колекцією книг, розроблений на Java з використанням Swing.

## Архітектура

```
┌─────────────────────────────────────────────────┐
│                  Машина користувача              │
│                                                  │
│   ┌───────────────┐        ┌──────────────────┐  │
│   │   BookGUI     │ ──────▶│    Catalogue     │  │
│   │  (Swing UI)   │        │  (бізнес-логіка) │  │
│   │  View+Control │ ◀───── │  CRUD / пошук    │  │
│   └───────────────┘        └────────┬─────────┘  │
│                                     │             │
│                            ┌────────▼─────────┐  │
│                            │   Файлова система │  │
│                            │  *.dat  *.csv     │  │
│                            └──────────────────┘  │
└─────────────────────────────────────────────────┘
```

**Компоненти:**
- Веб-сервер: відсутній (десктоп)
- Application server: відсутній
- СУБД: відсутня — дані зберігаються у `.dat`-файлах через Java Serialization
- Файлове сховище: локальна файлова система (`.dat` для каталогу, `.csv` для експорту)
- Сервіси кешування: відсутні

## Функціонал

- Додавання, редагування та видалення книг
- Пошук за назвою та автором
- Фільтрація за жанром
- Збереження каталогу у бінарний `.dat`-файл та завантаження з нього
- Експорт у `.csv`
- Статистика колекції

## Структура проєкту

```
Library/
├── src/
│   ├── Publication.java            # Базовий клас публікації
│   ├── Book.java                   # Клас книги
│   ├── BookNotFoundException.java  # Виняток
│   ├── Catalogue.java              # Бізнес-логіка
│   └── BookGUI.java                # Swing UI (точка входу)
├── docs/
│   ├── architecture.md             # Опис архітектури
│   ├── deployment.md               # Розгортання у production
│   ├── update.md                   # Процедура оновлення
│   ├── backup.md                   # Резервне копіювання
│   ├── generate_docs.md            # Генерація Javadoc
│   ├── javadoc/                    # Згенерована HTML-документація
│   └── scripts/                    # Скрипти автоматизації
│       ├── build.sh                # Компіляція + пакування JAR
│       ├── run.sh                  # Запуск (production)
│       ├── run-dev.sh              # Запуск (розробка)
│       ├── backup.sh               # Резервне копіювання
│       └── restore.sh              # Відновлення з резервної копії
├── build.sh                        # Швидка збірка + Javadoc
├── .github/workflows/build.yml     # CI/CD (GitHub Actions)
├── .gitignore
├── LICENSE
└── README.md
```

---

## Початок роботи (для розробника)

> Починаємо з нуля — свіжа ОС, нічого не встановлено.

### Крок 1 — Встановити JDK 17

**macOS (Homebrew):**
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**Ubuntu / Debian:**
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
```

**Windows:**
Завантажити інсталятор з https://adoptium.net та встановити `Eclipse Temurin 17 (LTS)`.

Перевірити встановлення:
```bash
java -version
# очікувано: openjdk version "17.x.x"
javac -version
# очікувано: javac 17.x.x
```

### Крок 2 — Клонувати репозиторій

```bash
git clone https://github.com/MariaNazarenkoStud/Library.git
cd Library
```

### Крок 3 — Скомпілювати проєкт

```bash
mkdir -p out
javac -d out src/*.java
```

Успішне виконання: немає виводу, з'явилася директорія `out/` з `.class`-файлами.

### Крок 4 — Запустити застосунок

```bash
java -cp out BookGUI
```

Відкриється вікно «Book Catalogue».

**Або одною командою (скрипт):**
```bash
chmod +x docs/scripts/run-dev.sh
./docs/scripts/run-dev.sh
```

### Крок 5 — Основні операції

| Операція | Як виконати |
|---|---|
| Додати книгу | Заповнити поля у верхній частині → «Add book» |
| Видалити книгу | Вписати назву → «Remove book» |
| Зберегти каталог | «Save to file» → вибрати шлях (зберігається як `.dat`) |
| Завантажити каталог | «Load from file» → вибрати `.dat`-файл |
| Експорт у CSV | «Export CSV» → вибрати шлях |
| Пошук | Поле «By title» або «By author» → «Search» |

### Крок 6 — Зібрати виконуваний JAR (опційно)

```bash
chmod +x docs/scripts/build.sh
./docs/scripts/build.sh
# Результат: dist/BookCatalogue.jar
```

Запуск JAR:
```bash
java -jar dist/BookCatalogue.jar
```

---

## Стандарти документування

Усі публічні класи та методи **обов'язково** мають містити Javadoc-коментарі:

- **Класи** — опис призначення, архітектурної ролі, приклад використання (`<pre>{@code ... }</pre>`)
- **Методи** — опис поведінки (не реалізації); теги `@param`, `@return`, `@throws`
- **Архітектурні рішення** — пояснення *чому* (inline-коментарі для неочевидної логіки)
- **`@see`** — посилання на пов'язані класи

Генерація HTML-документації:
```bash
javadoc -d docs/javadoc -sourcepath src -encoding UTF-8 src/*.java
```

Повна інструкція: [`docs/generate_docs.md`](docs/generate_docs.md)

---

## Ліцензія

Проєкт розповсюджується під ліцензією MIT — див. файл [LICENSE](LICENSE).
