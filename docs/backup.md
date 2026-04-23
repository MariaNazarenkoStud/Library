# Backup Guide — Book Catalogue

> Рекомендації для release engineer / DevOps щодо резервного копіювання даних.

---

## 1. Стратегія резервного копіювання

### Що необхідно резервувати

| Об'єкт | Розташування | Пріоритет |
|---|---|---|
| Файли каталогу (`.dat`) | Директорія запуску застосунку | **Критичний** — основні дані |
| Дистрибутивний JAR | `dist/BookCatalogue.jar` | Високий — для відкату |
| CSV-експорти | Директорія запуску | Середній — похідні дані |
| Конфігурація | відсутня (немає окремих файлів) | — |

### Типи резервних копій

| Тип | Опис | Застосування |
|---|---|---|
| **Повна (Full)** | Копія всіх `.dat` та JAR файлів | Щотижня або перед оновленням |
| **Інкрементальна** | Тільки змінені `.dat` файли (за датою модифікації) | Щодня для активних користувачів |
| **Snapshot перед оновленням** | Повна копія безпосередньо перед зміною JAR | Завжди перед оновленням |

### Частота резервного копіювання

| Сценарій | Частота |
|---|---|
| Активне використання (> 10 змін на день) | Щодня |
| Звичайне використання | Раз на тиждень |
| Перед будь-яким оновленням застосунку | Обов'язково |

### Зберігання та ротація

- Зберігати **мінімум 10 останніх резервних копій**
- Зберігати **щонайменше 1 snapshot перед останнім оновленням** назавжди
- Бекапи зберігаються у `~/BookCatalogue-backups/<timestamp>/`
- Автоматична ротація: скрипт видаляє копії старіші за 10 останніх (div. `backup.sh`)

---

## 2. Процедура резервного копіювання

### 2.1. Автоматизований backup (рекомендовано)

```bash
chmod +x docs/scripts/backup.sh
./docs/scripts/backup.sh
```

Що робить скрипт:
1. Знаходить усі `.dat`-файли в директорії проєкту
2. Копіює поточний `dist/BookCatalogue.jar`
3. Копіює `.csv`-файли (якщо є)
4. Записує метадані: час, хост, кількість файлів
5. Видаляє зайві старі копії (залишає 10 останніх)

Бекапи зберігаються у:
```
~/BookCatalogue-backups/
├── 20260115_143000/
│   ├── mycatalogue.dat
│   ├── BookCatalogue.jar
│   └── backup-info.txt
├── 20260120_090000/
│   └── ...
```

### 2.2. Ручне резервне копіювання

```bash
BACKUP_DIR="$HOME/BookCatalogue-backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"

# Файли даних
cp -v *.dat "$BACKUP_DIR/" 2>/dev/null || echo "no .dat files"

# JAR
cp -v dist/BookCatalogue.jar "$BACKUP_DIR/BookCatalogue.jar" 2>/dev/null || true

echo "Backup saved to: $BACKUP_DIR"
```

### 2.3. Автоматизація через cron (Linux / macOS)

Для щоденного автоматичного бекапу о 02:00:

```bash
crontab -e
```

Додати рядок:
```cron
0 2 * * * /path/to/Library/docs/scripts/backup.sh >> "$HOME/BookCatalogue-backups/backup.log" 2>&1
```

Для запуску кожні 6 годин:
```cron
0 */6 * * * /path/to/Library/docs/scripts/backup.sh >> "$HOME/BookCatalogue-backups/backup.log" 2>&1
```

### 2.4. Автоматизація через Task Scheduler (Windows)

```powershell
$action = New-ScheduledTaskAction -Execute "java" `
    -Argument "-cp C:\Library\out BookGUI" `
    -WorkingDirectory "C:\Library"

# Для backup — викликати bash-скрипт або PowerShell аналог:
$trigger = New-ScheduledTaskTrigger -Daily -At "02:00"
Register-ScheduledTask -Action $action -Trigger $trigger -TaskName "BookCatalogueBackup"
```

---

## 3. Перевірка цілісності резервних копій

### Перевірка наявності файлів

```bash
LATEST=$(ls -td "$HOME/BookCatalogue-backups/"* | head -1)
echo "Latest backup: $LATEST"
ls -lh "$LATEST/"
cat "$LATEST/backup-info.txt"
```

### Перевірка цілісності JAR

```bash
LATEST=$(ls -td "$HOME/BookCatalogue-backups/"* | head -1)

# Перевірити що JAR коректний
java -jar "$LATEST/BookCatalogue.jar" --check 2>/dev/null || \
    unzip -t "$LATEST/BookCatalogue.jar" > /dev/null && echo "JAR OK"
```

### Перевірка `.dat`-файлу (тестове завантаження)

```bash
# Скопіювати .dat з бекапу у тимчасову директорію, запустити застосунок
# і перевірити що він завантажується без помилок.
cp "$LATEST/mycatalogue.dat" /tmp/test-restore.dat
# Відкрити застосунок → Load from file → /tmp/test-restore.dat
```

---

## 4. Процедура відновлення з резервних копій

### 4.1. Повне відновлення системи

```bash
# Використати скрипт відновлення
chmod +x docs/scripts/restore.sh
./docs/scripts/restore.sh

# Або вказати конкретний бекап
./docs/scripts/restore.sh "$HOME/BookCatalogue-backups/20260120_090000"
```

Скрипт:
1. Показує інформацію про обраний бекап
2. Запитує підтвердження
3. Зберігає поточні `.dat`-файли як `.pre-restore-<time>` (на випадок помилки)
4. Копіює `.dat`-файли з бекапу
5. (Опційно) відновлює JAR-файл

### 4.2. Вибіркове відновлення даних

Якщо потрібно відновити лише один `.dat`-файл:

```bash
BACKUP_DIR="$HOME/BookCatalogue-backups/20260120_090000"
# Зберегти поточний файл
cp mycatalogue.dat mycatalogue.dat.bak
# Відновити конкретний файл
cp "$BACKUP_DIR/mycatalogue.dat" ./mycatalogue.dat
echo "File restored"
```

### 4.3. Тестування відновлення (рекомендовано щомісяця)

```bash
# 1. Зробити тестовий бекап
./docs/scripts/backup.sh /tmp/test-backup

# 2. Видалити тимчасові тестові дані
rm -f /tmp/test-restore-*.dat

# 3. Відновити у тимчасову директорію
cp "$HOME/BookCatalogue-backups"/*.dat /tmp/test-restore.dat 2>/dev/null || true

# 4. Запустити застосунок і завантажити /tmp/test-restore.dat
# Якщо дані коректні — тест пройдено

echo "Restore test: MANUAL CHECK REQUIRED — open app and load /tmp/test-restore.dat"
```

---

## 5. Що НЕ потребує резервного копіювання

| Об'єкт | Причина |
|---|---|
| Вихідний код (`src/`) | Зберігається у Git (GitHub) |
| `out/` (скомпільовані класи) | Регенерується командою `javac` |
| `docs/javadoc/` | Регенерується командою `javadoc` |
| `node_modules/`, `.DS_Store` | Виключені через `.gitignore` |
