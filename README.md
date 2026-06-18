# 🧊 ReplaceCraft

> **Minecraft Classic, переписанный на Java 1.8 (SE) с OpenGL 1.1 с помощью нейросетей.**

[![Java Version](https://img.shields.io/badge/Java-1.8-blue.svg)](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
[![OpenGL](https://img.shields.io/badge/OpenGL-1.1-green.svg)](https://www.opengl.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## Особенности

- **Открытый код** — С помощью него открываются интересные особенности
- **Сохранение миров** — На F5 ты можешь сохранить свои постройки в файл `.rc`
- **Читерские способности** — NoClip и Fly, ты сможешь осмотреть свою постройку!
- **Мультиплеер** — Самое базовое для игры с друзьями (нестабильно, не защищено, играйте на свой страх и риск)
- **Оптимизировано** — Выдаёт сравнимые с оригинальным клиентом Minecraft Classic показатели

---

## Управление

- **WASD** — Передвижение
- **Левая кнопка мыши** — Разбить блок
- **Правая кнопка мыши** — Установить блок
- **1–7** — Выбрать блок
- **`** (grave) — Debug Menu
- **F1** — Fly
- **F2** — NoClip
- **F3** — Полноэкранный режим
- **F5** — Сохранить мир
- **F9** — Загрузить мир
- **F10** — Подключиться к серверу
- **F11** — Отключиться

---

## Запуск

### Требования
- Java 8 (JRE/JDK)
- LWJGL 2.9.3
- Видеокарта с базовой поддержкой OpenGL 1.1

### Сборка и запуск
1. Клонируй репозиторий
2. Загрузи LWJGL 2.9.3 с сайта [legacy.lwjgl.org](https://legacy.lwjgl.org)
3. Распакуй файлы `lwjgl.jar`, `lwjgl_util.jar` и нативные библиотеки в папку `libs/`
4. Открой проект в Eclipse
5. Добавь `libs/*.jar` в путь сборки
6. Установи параметр `-Djava.library.path=libs/native/windows` в конфигурации запуска

### Запуск клиента
```bat
java -Djava.library.path="libs/native/windows" -jar ReplaceCraft.jar
