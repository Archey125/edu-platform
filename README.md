# Учебная платформа (edu-platform)

Итоговый проект по курсу **«ORM-фреймворки для Java»**: веб-приложение на **Spring Boot 3** с доступом к **PostgreSQL** через **Hibernate/JPA (Spring Data JPA)**. Реализует хранение структуры курсов и учебных материалов, управление пользователями (студенты/преподаватели/администраторы) и процессы обучения — запись на курс, сдача домашних заданий, прохождение тестов.

**Проверено в CI:** `mvn test` — **101 тест (unit + интеграционные), 100 % проходят успешно.**

---

## 1. Технологический стек

| Компонент | Технология |
|---|---|
| Язык | Java 17 |
| Фреймворк | Spring Boot 3.3.5 (Web, Data JPA, Validation) |
| ORM | Hibernate 6.5 (JPA-провайдер) |
| СУБД | PostgreSQL 16 (продакшен, профиль `dev`) |
| Тесты | H2 in-memory в режиме PostgreSQL (профиль `test`), JUnit 5, Mockito, AssertJ, MockMvc |
| Документация API | Springdoc OpenAPI (Swagger UI) |
| Сборка | Maven (+ Maven Wrapper `./mvnw`) |
| Инфраструктура | Dockerfile, docker-compose, GitHub Actions (CI) |

---

## 2. Архитектура

Классическая многослойная архитектура, код разделён по пакетам:

```
com.example.eduplatform
├── entity        — 17 JPA-сущностей + 5 enum (аннотации связей, каскады, LAZY)
├── repository    — 16 репозиториев Spring Data JPA (CRUD + JOIN FETCH + @EntityGraph)
├── service       — бизнес-логика (границы транзакций, проверки правил, уведомления)
├── web           — 10 REST-контроллеров
├── dto           — request/response record-классы с валидацией Jakarta Validation
├── exception     — свои исключения + GlobalExceptionHandler (@RestControllerAdvice)
└── config        — DataSeeder (предзаполнение демо-данных)
```

**Ключевые архитектурные решения**

- **`spring.jpa.open-in-view: false`** — ленивые связи недоступны вне транзакции «честно». Все контроллеры отдают DTO, данные подтягиваются в сервисах через JOIN FETCH — см. раздел «Ленивая загрузка».
- **DTO вместо прямого сериализования сущностей** — исключает `LazyInitializationException` в JSON и не раскрывает правильные ответы тестов.
- **Централизованная обработка ошибок** — `GlobalExceptionHandler` возвращает единый JSON: 400 (валидация, бизнес-правила), 404 (не найдено), 409 (дубликаты, конфликты удаления), 500.
- **Профили конфигурации** — `dev` (PostgreSQL, параметры из переменных окружения) и `test` (H2, `create-drop`).

---

## 3. Модель данных — 17 сущностей

| # | Сущность | Таблица | Связи |
|---|---|---|---|
| 1 | **User** | users | 1-1 Profile; 1-M coursesTaught, Enrollment, Submission, QuizSubmission, CourseReview, Notification |
| 2 | **Profile** | profiles | 1-1 User (FK user_id unique) — «редко используемые данные отдельно» |
| 3 | **Category** | categories | 1-M Course |
| 4 | **Course** | courses | M-1 Category, M-1 User(teacher); 1-M Module; M-M Tag через course_tags; 1-M Enrollment, CourseReview |
| 5 | **Enrollment** | enrollments | M-1 User, M-1 Course — промежуточная сущность M-M (unique student+course, статус, дата) |
| 6 | **Module** | modules | M-1 Course; 1-M Lesson; 1-1 Quiz |
| 7 | **Lesson** | lessons | M-1 Module; 1-M Assignment |
| 8 | **Assignment** | assignments | M-1 Lesson; 1-M Submission |
| 9 | **Submission** | submissions | M-1 Assignment, M-1 User — M-M с данными (unique assignment+student, score, feedback) |
| 10 | **Quiz** | quizzes | 1-1 Module (unique module_id); 1-M Question |
| 11 | **Question** | questions | M-1 Quiz; 1-M AnswerOption |
| 12 | **AnswerOption** | answer_options | M-1 Question (is_correct) |
| 13 | **QuizSubmission** | quiz_submissions | M-1 Quiz, M-1 User (unique quiz+student, score, passed); 1-M QuizAnswer |
| 14 | **QuizAnswer** | quiz_answers | M-1 QuizSubmission, M-1 Question, M-1 AnswerOption — сохраняет выбранные ответы |
| 15 | **CourseReview** | course_reviews | M-1 Course, M-1 User (unique course+student, rating 1–5) |
| 16 | **Tag** | tags | M-M Course (обратная сторона @ManyToMany) |
| 17 | **Notification** | notifications | M-1 User |

Покрыты все три типа связей: **1-1** (User–Profile, Module–Quiz), **1-M** (Course→Module→Lesson→Assignment→Submission, Quiz→Question→AnswerOption и др.), **M-M** (User↔Course через сущность Enrollment и Course↔Tag через @JoinTable).

### Правила каскадов и удаления

- **Контент курса каскадно удаляется вместе с ним**: Course → Module → Lesson → Assignment → Submission и Module → Quiz → Question → AnswerOption (`cascade = ALL, orphanRemoval = true`).
- **Курс с записанными студентами удалить нельзя (409)** — сохраняем историю обучения. Вариант «удалять вместе с прохождением» отброшен как небезопасный.
- **Удаление студента** удаляет его записи, решения, результаты тестов, отзывы, уведомления (явные bulk-удаления в `UserService.delete` — см. уроки про ORM ниже). **Преподавателя с курсами удалить нельзя (409)**.
- **На связях «многие ко многим» каскада нет** (Course↔Tag): тег, используемый в курсах, удалить нельзя.
- **Модуль с результатами по его тесту удалить нельзя (409)**.

---

## 4. Запуск

### Вариант 1 — Docker Compose (рекомендуется)

Требуется Docker. Поднимает PostgreSQL + приложение одной командой:

```bash
docker compose up --build
```

- Приложение: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- PostgreSQL: localhost:5432 (db `edu_platform`, user/password `postgres`)

### Вариант 2 — локально с установленным PostgreSQL

1. Создайте базу: `CREATE DATABASE edu_platform;`
2. Задайте переменные окружения (см. `.env.example`) или используйте значения по умолчанию:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/edu_platform
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
./mvnw spring-boot:run
```

Схема создаётся автоматически (`ddl-auto=update`, параметр `DDL_AUTO`), демо-данные засеваются при старте (`SEED_DATA=true`).

### Профили и переменные окружения

| Переменная | Назначение | По умолчанию (dev) |
|---|---|---|
| `DB_URL` | JDBC-URL PostgreSQL | `jdbc:postgresql://localhost:5432/edu_platform` |
| `DB_USERNAME` / `DB_PASSWORD` | учётные данные БД | `postgres` / `postgres` |
| `DDL_AUTO` | режим генерации схемы | `update` |
| `SEED_DATA` | предзаполнение демо-данными | `true` |
| `SQL_LOG_LEVEL` | логирование SQL (`DEBUG`) | `INFO` |

Никакие пароли и URL не захардкожены — только `application-dev.yml` с плейсхолдерами `${...}`.

### Демо-данные (DataSeeder)

При старте с пустой базой создаются: 4 категории, 6 тегов, 6 пользователей, 3 курса («ORM-фреймворки для Java», «Основы PostgreSQL», «UX-основы дизайна интерфейсов») с модулями, уроками, заданиями и тестом, 4 записи на курсы, 3 решения (с оценками), 2 результата теста, 2 отзыва и 4 уведомления. Повторный запуск идемпотентен (при непустой базе пропускается).

Демо-пользователи: `admin@edu.io` (ADMIN), `igor.smirnov@edu.io` / `elena.petrova@edu.io` (TEACHER), `maksim@edu.io`, `olga@edu.io`, `ivan@edu.io` (STUDENT). Аутентификации в базовой части нет — роли различаются бизнес-логикой, как и предусмотрено ТЗ.

---

## 5. REST API

Полный список эндпойнтов — в Swagger UI (`/swagger-ui.html`). Основные:

### Пользователи `/api/users`
| Метод | Путь | Описание |
|---|---|---|
| POST | `/api/users` | создать пользователя `{name, email, role}` → 201 |
| GET | `/api/users` | список пользователей |
| GET | `/api/users/{id}` | пользователь (404, если нет) |
| PUT | `/api/users/{id}` | частичное обновление |
| DELETE | `/api/users/{id}` | удалить (каскад активности; 409 для преподавателя с курсами) |
| GET / PUT | `/api/users/{id}/profile` | профиль (1-1) |
| GET | `/api/users/{id}/courses` | курсы студента (записи) |
| GET | `/api/users/{id}/submissions` | решения студента |
| GET | `/api/users/{id}/quiz-results` | результаты тестов |
| GET | `/api/users/{id}/notifications` | уведомления |

### Курсы `/api/courses`
| Метод | Путь | Описание |
|---|---|---|
| POST | `/api/courses` | создать курс `{title, description, categoryId, teacherId, ...}` → 201 |
| GET | `/api/courses?categoryId=&tagId=&teacherId=` | список с фильтрами |
| GET | `/api/courses/{id}` | полная структура: модули → уроки → задания |
| PUT | `/api/courses/{id}` | обновить (в т.ч. сменить преподавателя/категорию) |
| DELETE | `/api/courses/{id}` | удалить (409, если есть записанные студенты) |
| POST | `/api/courses/{id}/modules` | добавить модуль |
| POST / DELETE | `/api/courses/{id}/tags/{tagId}` | привязать/отвязать тег (M-M) |
| POST | `/api/courses/{id}/enroll` | записать студента `{userId}` |
| GET | `/api/courses/{id}/students` | студенты курса |
| GET / POST | `/api/courses/{id}/reviews` | отзывы (только записанным, один на курс) |

### Обучение
| Метод | Путь | Описание |
|---|---|---|
| POST | `/api/modules/{id}/lessons` | добавить урок в модуль |
| POST | `/api/lessons/{id}/assignments` | добавить задание к уроку |
| POST | `/api/assignments/{id}/submit` | отправить решение `{studentId, content, fileUrl}` → 201 (409 — повтор; 400 — дедлайн/не записан) |
| GET | `/api/assignments/{id}/submissions` | решения по заданию (преподавателю) |
| PUT | `/api/submissions/{id}/grade` | выставить оценку `{score, feedback}` (400 при score > maxScore) |
| POST | `/api/modules/{id}/quiz` | создать тест модуля `{title, questions: [{questionText, type, points, options: [{optionText, isCorrect}]}]}` |
| GET | `/api/quizzes/{id}` | тест с вопросами — **без `isCorrect`** |
| POST | `/api/quizzes/{id}/take` | пройти тест `{studentId, answers: {questionId: [optionIds]}}` → 201 (409 — пересдача) |
| GET | `/api/quizzes/{id}/results?studentId=` | результаты |
| GET / PUT / DELETE | `/api/enrollments...` | записи: списки `?courseId=` / `?studentId=`, смена статуса, отписка |

### Справочники
`/api/categories` (CRUD), `/api/tags` (создание/список/удаление — 409, если тег используется).

### Пример сценария (curl)

```bash
# Создать категорию, преподавателя и курс
curl -X POST localhost:8080/api/categories -H "Content-Type: application/json" \
  -d '{"name": "Программирование"}'
curl -X POST localhost:8080/api/users -H "Content-Type: application/json" \
  -d '{"name": "Игорь", "email": "igor@edu.io", "role": "TEACHER"}'
curl -X POST localhost:8080/api/courses -H "Content-Type: application/json" \
  -d '{"title": "Основы Hibernate", "categoryId": 1, "teacherId": 2}'

# Записать студента, отправить решение, оценить
curl -X POST localhost:8080/api/courses/1/enroll -H "Content-Type: application/json" -d '{"userId": 4}'
curl -X POST localhost:8080/api/assignments/1/submit -H "Content-Type: application/json" \
  -d '{"studentId": 4, "content": "Моё решение"}'
curl -X PUT localhost:8080/api/submissions/1/grade -H "Content-Type: application/json" \
  -d '{"score": 95, "feedback": "Отлично!"}'
```

---

## 6. Ленивая загрузка — как это отработано

Все `@OneToMany`/`@ManyToMany` — LAZY (по спецификации, указано явно для ясности); все `@ManyToOne`/`@OneToOne` — **явно `fetch = LAZY`** (JPA по умолчанию делает их EAGER — здесь это переопределено). OSIV отключён, поэтому ленивые связи вне транзакции недоступны — и приложение с этим корректно работает за счёт:

1. **JOIN FETCH в JPQL** — например `CourseRepository.findWithFullStructureById` загружает курс сразу с модулями, уроками, заданиями и тестами одним запросом.
2. **`@EntityGraph`** — `CourseRepository.findWithEnrollmentsById` подтягивает записи студентов вместе со студентами.
3. **Транзакционные методы сервиса** — DTO собираются внутри `@Transactional`-метода, где ленивые прокси ещё инициализируемы.
4. **Сортировка Set-коллекций в маппинге** — порядок элементов Set не гарантирован, DTO сортируются по `orderIndex`/id.

**Тест-демонстрация** — `LazyLoadingTest`:
- обращение к `course.getModules()` вне транзакции → `LazyInitializationException` (ленивость доказана);
- `course.getTeacher()` после `findById` — не инициализированный прокси (`Hibernate.isInitialized == false`) — `@ManyToOne` действительно LAZY;
- те же коллекции после JOIN FETCH / @EntityGraph доступны без ошибок;
- внутри `TransactionTemplate` коллекции читаются штатно.

**Урок, который дала разработка:** полагаться на каскад LAZY-коллекций при удалении «надёжно» нельзя — внутри одного persistence context коллекции уже сохранённой сущности содержат только то, что добавлено через неё; строки, вставленные напрямую, каскад не увидит. Поэтому `UserService.delete` удаляет зависимости **явными bulk-запросами** в детерминированном порядке, а проверка «у преподавателя есть курсы» выполняется запросом `existsByTeacherId`, а не чтением коллекции. Все эти ситуации покрыты тестами (`UserCascadeTest`).

---

## 7. Тестирование

**101 тест** — unit + интеграционные, все проходят: `mvn test` (или `./mvnw test`).

| Класс тестов | Что проверяет |
|---|---|
| `SchemaAndEntitiesTest` | создание схемы БД по моделям (все 18 таблиц), каскадное сохранение графа «курс → модуль → урок → задание» одним `save`, генерация ключей, unique-индексы (email), хранение enum строками |
| `LazyLoadingTest` | демонстрация LazyInitializationException, LAZY-прокси у @ManyToOne, решения: JOIN FETCH, @EntityGraph, TransactionTemplate |
| `CourseCascadeTest` | CRUD курса, каскадное удаление контента, запрет удаления с записями (409), теги M-M (attach/detach/дубликат), фильтры по категории/преподавателю |
| `EnrollmentServiceIntegrationTest` | запись/отписка/статусы, уникальность (409), роли (только STUDENT), списки в обе стороны, уведомление преподавателю |
| `SubmissionServiceIntegrationTest` | отправка решения, запрет дубликата/просрочки/не-записанного, оценивание, уведомления, удаление и пересдача |
| `QuizServiceIntegrationTest` | создание теста (валидация вопросов), прохождение с автоподсчётом, запрет пересдачи, сокрытие правильных ответов, результаты |
| `QuizScorerTest` (unit) | правила подсчёта: SINGLE/MULTIPLE_CHOICE, частичный выбор, суммы баллов |
| `DataSeederTest` | предзаполнение при старте, идемпотентность |
| `UserCascadeTest` | каскадное удаление пользователя, запрет удаления преподавателя с курсами, upsert профиля |
| `UserApiTest`, `CourseApiTest`, `EnrollmentApiTest`, `SubmissionApiTest`, `QuizApiTest` | REST API через MockMvc: коды 201/200/204/400/404/409, валидация полей, JSON-структуры |

Интеграционные тесты используют **H2 in PostgreSQL-режиме** (профиль `test`, `ddl-auto=create-drop`) — для запуска тестов не нужен установленный PostgreSQL. Приложение же работает только на PostgreSQL (H2 в main-код не входит).

### Как посмотреть отчёт

```bash
mvn test                          # сводка "Tests run: 101, Failures: 0, Errors: 0"
mvn surefire-report:report        # HTML-отчёт: target/site/surefire-report.html
```

- Консольная сводка — в конце вывода `mvn test` по каждому классу: `Tests run: X, Failures: 0, Errors: 0, Skipped: 0`.
- Подробные XML/текстовые отчёты: `target/surefire-reports/`.
- HTML-отчёт: `mvn surefire-report:report`, затем открыть `target/site/surefire-report.html`.
- В IDE (IntelliJ IDEA): зелёные галочки у всех тестов в дереве Run.

---

## 8. Сборка и CI

```bash
./mvnw clean package    # собрать jar: target/edu-platform-1.0.0.jar
mvn verify              # сборка + все тесты
```

GitHub Actions (`.github/workflows/ci.yml`): при каждом push/PR — сборка, прогон 101 теста, публикация отчётов артефактом и отдельная сборка Docker-образа.

---

## 9. Структура проекта

```
edu-platform/
├── pom.xml                        # Maven: Spring Boot 3.3.5, Java 17
├── Dockerfile                     # многоэтапная сборка (Maven → JRE)
├── docker-compose.yml             # PostgreSQL 16 + приложение
├── .github/workflows/ci.yml       # CI: тесты + Docker build
├── mvnw, mvnw.cmd                 # Maven Wrapper (сборка без установленного Maven)
├── .env.example                   # пример переменных окружения
└── src
    ├── main/java/com/example/eduplatform/
    │   ├── EduPlatformApplication.java
    │   ├── config/DataSeeder.java          # демо-данные
    │   ├── entity/          (17 сущностей + enums/)
    │   ├── repository/      (16 репозиториев)
    │   ├── service/         (12 сервисов + QuizScorer)
    │   ├── web/             (10 контроллеров)
    │   ├── dto/             (request/response records)
    │   └── exception/       (NotFoundException, DuplicateResourceException,
    │                         BusinessRuleException, ConflictException,
    │                         ErrorResponse, GlobalExceptionHandler)
    ├── main/resources/
    │   ├── application.yml          # общие настройки, OSIV=false
    │   ├── application-dev.yml      # PostgreSQL через переменные окружения
    │   └── application-test.yml     # H2 для интеграционных тестов
    └── test/java/com/example/eduplatform/
        ├── api/       (5 классов MockMvc-тестов REST API)
        ├── integration/ (9 классов интеграционных тестов)
        ├── unit/QuizScorerTest.java
        └── support/TestDataFactory.java
```
