# Update Guide — Book Catalogue

> Покрокові рекомендації для release engineer / DevOps щодо оновлення застосунку.

---

## 1. Підготовка до оновлення

### 1.1. Перевірка сумісності

Перед оновленням перевірити:

```bash
# Поточна версія JAR на машині користувача
unzip -p BookCatalogue.jar META-INF/MANIFEST.MF | grep Implementation-Version

# Версія JRE на машині користувача
java -version
```

| Перевірка | Умова для продовження |
|---|---|
| JRE версія | ≥ 17 (нові версії JRE зворотно сумісні) |
| Сумісність `.dat` | Якщо змінилися поля класів `Book` або `Publication` — потрібна міграція даних (див. п. 3.3) |
| Вільне місце на диску | ≥ 50 MB |

### 1.2. Планування часу простою

Застосунок є десктопним — час простою мінімальний (хвилини). Рекомендовано:
- Повідомити користувача про оновлення заздалегідь
- Попросити зберегти і закрити застосунок перед оновленням
- Обрати момент, коли користувач не працює з даними

### 1.3. Резервна копія перед оновленням

**Обов'язково** перед будь-яким оновленням:

```bash
chmod +x docs/scripts/backup.sh
./docs/scripts/backup.sh
```

Або вручну — скопіювати всі `.dat`-файли в безпечне місце:

```bash
BACKUP_DIR="$HOME/BookCatalogue-backup-$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp -v *.dat "$BACKUP_DIR/" 2>/dev/null || echo "No .dat files found"
cp -v BookCatalogue.jar "$BACKUP_DIR/BookCatalogue-old.jar"
echo "Backup saved to: $BACKUP_DIR"
```

---

## 2. Процес оновлення

### 2.1. Зупинка застосунку

Попросити користувача закрити вікно застосунку.
Або завершити процес примусово:

```bash
# macOS / Linux
pkill -f "BookCatalogue.jar" && echo "Stopped"

# Windows (PowerShell)
Stop-Process -Name java -Force
```

### 2.2. Отримання нової версії

**Спосіб A — завантажити готовий JAR (release):**

```bash
# Завантажити з GitHub Releases
curl -L -o BookCatalogue-new.jar \
  "https://github.com/MariaNazarenkoStud/Library/releases/latest/download/BookCatalogue.jar"
```

**Спосіб B — зібрати з вихідного коду:**

```bash
cd /path/to/Library
git fetch origin
git checkout main
git pull origin main
./docs/scripts/build.sh
# Результат: dist/BookCatalogue.jar
```

### 2.3. Розгортання нового JAR

```bash
# Замінити старий JAR новим
cp dist/BookCatalogue.jar /path/to/install/BookCatalogue.jar

# Або при оновленні на машині користувача
cp BookCatalogue-new.jar BookCatalogue.jar
```

### 2.4. Міграція даних (якщо потрібно)

Міграція потрібна лише якщо змінилась структура класів `Book`, `Publication` або `Catalogue`
(додані/видалені поля). Перевірити в `CHANGELOG.md` або в diff між версіями.

**Якщо сумісність збережено (minor update):**
Міграція не потрібна. `.dat`-файли продовжують працювати.

**Якщо сумісність порушена (major update):**

1. Запустити стару версію застосунку
2. Завантажити `.dat`-файл
3. Експортувати у CSV: «Export CSV»
4. Запустити нову версію застосунку
5. Вручну додати дані з CSV або написати скрипт міграції

### 2.5. Запуск після оновлення

```bash
java -jar BookCatalogue.jar
```

---

## 3. Перевірка після оновлення

### Чекліст перевірки:

- [ ] Застосунок запускається без помилок
- [ ] Можна додати нову книгу
- [ ] Можна зберегти каталог у `.dat`-файл
- [ ] Існуючий `.dat`-файл завантажується і відображає коректні дані
- [ ] Пошук за назвою та автором працює
- [ ] Фільтрація за жанром працює
- [ ] Експорт у CSV працює

---

## 4. Процедура відкату (+1 бал)

Якщо оновлення виявилось невдалим (застосунок не запускається, дані не читаються):

### Крок 1 — Зупинити новий застосунок

```bash
pkill -f "BookCatalogue.jar"
```

### Крок 2 — Відновити резервну копію

```bash
# Знайти останню резервну копію
ls -td "$HOME/BookCatalogue-backup-"* | head -1

# Або скористатись скриптом відновлення
chmod +x docs/scripts/restore.sh
./docs/scripts/restore.sh
```

Вручну:
```bash
BACKUP_DIR=$(ls -td "$HOME/BookCatalogue-backup-"* | head -1)
echo "Restoring from: $BACKUP_DIR"

# Відновити старий JAR
cp "$BACKUP_DIR/BookCatalogue-old.jar" BookCatalogue.jar

# Відновити файли даних
cp -v "$BACKUP_DIR"/*.dat . 2>/dev/null || echo "No .dat backups found"
echo "Rollback complete"
```

### Крок 3 — Перевірити відкат

```bash
java -jar BookCatalogue.jar
```
Застосунок повинен запуститися зі старими даними.

### Крок 4 — Повідомити команду

- Задокументувати що саме пішло не так
- Відкрити issue у GitHub: https://github.com/MariaNazarenkoStud/Library/issues
- Вказати версію, ОС, Java, повідомлення про помилку

---

## 5. Журнал оновлень

| Версія | Дата | Тип | Сумісність `.dat` |
|---|---|---|---|
| 1.0.0 | 2026-01 | Initial release | — |
| 1.1.0 | 2026-02 | Minor (пошук за автором, жанр-фільтр) | ✅ Сумісна |
| 1.2.0 | 2026-03 | Minor (CSV-експорт, статистика) | ✅ Сумісна |
