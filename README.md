# TagFileSystem
Тэговая файловая система на Java для ОС Windows c визуализацией.

![screen1](https://disk.yandex.ru/i/UUKO1N4RuV4hMg)

Визуализация
![screen1](https://github.com/vnikolaenko-dev/TagFileSystem/raw/main/screenshots/screen1.jpg)
Редактор файлов и тэгов
![screen2](https://github.com/vnikolaenko-dev/TagFileSystem/raw/main/screenshots/screen2.jpg)


#1. Общее описание
- Проект TagFileSystem представляет собой визуализатор файловой системы с использованием тегов для классификации и поиска файлов.
- Основная цель проекта — предоставить пользователю удобный графический интерфейс для работы с файлами, а также эффективные средства поиска и организации файлов с помощью меток (тегов). 
- Проект использует JavaFX для графического интерфейса и интегрируется с базой данных для хранения тегов и других данных о файлах.


#2. Функциональные возможности
Проект реализует следующие ключевые функции:
- Визуализация файловой системы: Все файлы и папки отображаются на холсте в виде узлов (FileNode), которые можно перемещать и манипулировать ими.
- Использование тегов для классификации файлов: Пользователи могут присваивать файлам теги и использовать их для упорядочивания и поиска.
- Поиск файлов по тегам: Предоставляется возможность поиска файлов по их тегам.
- Редактирование тегов: Пользователи могут добавлять, удалять и изменять теги для файлов через интерфейс.
- Динамическая загрузка данных: Приложение автоматически отслеживает изменения в файловой системе, и при изменении содержимого директорий обновляет представление на экране.
- Интеграция с базой данных: Приложение использует базу данных для хранения информации о тегах и связях между файлами и тегами.


#3. Технологии и архитектура
- Java: Язык программирования, на котором реализован проект.
- JavaFX: Библиотека для создания графического интерфейса пользователя (GUI). Используется для визуализации файловой системы, создания окон и компонентов управления.
- ODatabaseSession (OrientDB): База данных, которая используется для хранения информации о файлах и тегах. Эта база данных предоставляет быстрый доступ к данным и обеспечивает сохранность информации.
- CSS: Используются для описания пользовательского интерфейса и стилизации компонентов.


#4. Основные компоненты и их описание
- MainApp: Главный класс приложения, который инициализирует все компоненты, запускает приложение и управляет взаимодействием с пользователем.
- FileNode: Класс, представляющий файл или директорию в системе. Каждый узел имеет связанные с ним метки, координаты и другие метаданные.
- CanvasComponent: Класс, отвечающий за отображение и обновление состояния файлов на холсте. Он управляет позициями файлов и их визуализацией.
- FileService: Класс для работы с файлами и их метками. Включает методы для добавления, удаления и поиска файлов по тегам.
- TagService: Сервис для управления тегами в базе данных.
- EditFileDialog и EditTagDialog: Диалоговые окна для редактирования тегов и файлов, которые позволяют пользователю добавлять, редактировать и удалять теги.
- Alert: Утилитарный класс для отображения окон с сообщениями и ошибками.


#5. Принцип работы
Проект работает в интерактивном режиме, где пользователи могут выполнять следующие действия:
- Загружать и отображать файлы с их метками в интерфейсе.
- Присваивать и удалять теги для файлов.
- Искать файлы по тегам.
- Просматривать подробную информацию о файле, включая путь, размер и связанные теги.
- При необходимости редактировать или открывать файлы.
- Все изменения сохраняются в базе данных.
- При изменении содержимого папок (например, добавление или удаление файлов), приложение автоматически обновляет отображаемые данные, что позволяет поддерживать актуальность визуализации.


#6. Пример использования
- После запуска приложения на экране отображаются все файлы в выбранной папке.
- Пользователь может выбрать файл и добавить к нему теги с помощью интерфейса.
- Для поиска файлов по тегам достаточно ввести нужный тег в поле поиска, и приложение отобразит все файлы с этим тегом.
- Для удаления файла или его тегов можно использовать контекстное меню или диалоговые окна.


#7. Примечания и особенности
- Проект предназначен для удобного взаимодействия с файлами и метками. Он ориентирован на пользователей, которым необходимо классифицировать файлы с помощью тегов.
- Для хранения данных используется база данных, что позволяет сохранять изменения даже после закрытия приложения.
- Все изменения и взаимодействия с приложением (например, добавление тегов или редактирование файлов) происходят через пользовательский интерфейс, что делает использование приложения интуитивно понятным.

