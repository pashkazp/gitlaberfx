# GitLab Branch Manager - Інструкції запуску

## Вимоги

- Java 17 або вище
- Maven 3.6 або вище
- Доступ до GitLab інстансу

## Запуск додатку

### Windows

Через обмеження безпеки у Windows, додаток необхідно запускати спеціальним чином:

1. Відкрийте командний рядок (cmd) або PowerShell
2. Перейдіть до директорії проекту
3. Виконайте команду:
   ```
   mvn clean compile antrun:run@run-with-debug
   ```

Або використовуйте готовий скрипт:
```
run.bat
```

### Linux

1. Відкрийте термінал
2. Перейдіть до директорії проекту
3. Зробіть скрипт виконуваним (тільки перший раз):
   ```
   chmod +x run.sh
   ```
4. Запустіть додаток:
   ```
   ./run.sh
   ```

Або виконайте команду напряму:
```
mvn clean compile antrun:run@run-with-debug
```

## Відлагодження

Додаток запускається з увімкненим debug портом 5005. Для підключення відлагоджувача:

### IntelliJ IDEA

1. Run → Edit Configurations
2. Додайте нову конфігурацію Remote JVM Debug
3. Встановіть:
   - Host: localhost
   - Port: 5005
4. Запустіть додаток через Maven
5. Запустіть debug конфігурацію в IDE

### Visual Studio Code

1. Створіть файл `.vscode/launch.json`:
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug GitLaberFXC",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
        }
    ]
}
```
2. Запустіть додаток через Maven
3. Натисніть F5 для підключення відлагоджувача

## Вирішення проблем

### Windows: "JavaFX runtime components are missing"

Ця помилка виникає при спробі запустити додаток стандартним способом. Використовуйте тільки команду Maven, вказану вище.

### Linux: "Permission denied"

Переконайтеся, що скрипт `run.sh` має права на виконання:
```
chmod +x run.sh
```

### Загальні проблеми

1. Переконайтеся, що використовується Java 17:
   ```
   java -version
   ```

2. Переконайтеся, що Maven встановлено:
   ```
   mvn -version
   ```

3. Очистіть кеш Maven:
   ```
   mvn clean
   ```

## Конфігурація

При першому запуску додаток створить директорію `configs` з файлами налаштувань:
- `application.properties` - загальні налаштування
- `projects.properties` - налаштування проектів
- `logging.properties` - налаштування логування

Логи зберігаються в директорії `logs`.