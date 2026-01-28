# BuildThing by 51111n355
[![](https://jitpack.io/v/51111N355/buildthing.svg)](https://jitpack.io/#51111N355/buildthing)
- Инструментация для разделения Java, Kotlin проектов на стороны (И не только)

## Запланированный функционал, изменения
1. Анализатор кода - симулирует выполнение байткода, чтобы определить куда, что, откуда передается. Обязательно отличать если значение на стаке константа + поддерживать математику между константами.
2. Своя Dead Code Elimination на основе анализатора кода, удалять блоки кода где получается `if (C == false) { ... }`. Обычно это делает и компилятор Java, но из-за `Inject` может что-то остаться.
3. Переписать то что сейчас происходит с `Inject.xxx` на основе анализатора кода.
4. Переписать `@FlagCuttable` - Сделать `@RemoveAtCallsite` аргументом аннотации, что-то типо `@FlagCuttable(value = "...", removeCall=true, ...)`
   - `@FlagCuttable` не должен давать повесить его на котлин `inline` методы
   - Разобраться что происходит с `suspend` методами.
5. Опция "@FlagCuttable" - Оставить метод пустым вместо удаления, например для реализаций абстрактных методов, ситуации с рефлексией и т.д.
6. Полностью переделать обработку ошибок, сообщение о них во всем плагине
7. Flag Dispatch механзм - в зависимости от флага компилируется вызов в разные целевые методы вместо вызова пустого\`native` метода.
8. Переделать всё что связано с dev runtime. Мне не нравится ни подмена .jar, ни подмена `classes`. По идее это всё можно заменить на реальное редактирование настроек JavaExec тасков и Forge схавает.

## Возможности

### Kotlin DSL
Доступна Kotlin DSL для создания тасков и настройки плагина. 

Пример использования:

```kts
// Обязательно импортируем DSL пакет
import net.im51111n355.buildthing.dsl.*

buildthing {
    // Создает Client профиль сборки
    buildProfile("Client") {
        // Настройки
        config.flags.add("client")
    }

    // Создает Server профиль сборки
    buildProfile("Server") {
        // Настройки
        config.flags.add("server")
    }

    // Настраивает чтобы перед runDev таском обрабатывались классы
    processClassesBeforeTask("RunDev", runDev) {
        dependsOn("classes")
        config.values.put("some_value", 131313)
    }
    
    // Также есть processJarBeforeTask (см. документацию ниже)
}
```

### Groovy DSL
Доступна, отдельная от котлин, groovy DSL для создания тасков и настройки плагина.

Пример использования:

```groovy
// Импортировать ничего не надо =)
buildthing {
    // Создает Client профиль сборки
    buildProfile("Client", shadowJar) {
        // Настройки
        config.flags.add("client")
    }

    // Создает Server профиль сборки
    buildProfile("Server", shadowJar) {
        // Настройки
    }

    // Настраивает чтобы перед runClient таском обрабатывался jar
    // В отличии от kotlin DSL нужно указывать 3й параметр null !!
    processJarBeforeTask("DevRunClient", runClient, null) {
        config.values.put("some_value", 131313)
    }

    // Также есть processClassesBeforeTask (см. документацию ниже)
}
```

### Отключение вырезалки
Можно создавать билды с выключенным функционалом вырезания, но работающими Inject.flag (И всем остальным функционалом).

Используется самой вырезалкой.

Контроллируется настройкой `BuildThingConfig#disableCutter`, выключено по умолчанию. Исключения - `processJarBeforeTask`/`processClassesBeforeTask` таски, где эта настройка включена по умолчанию.

### Обработка для Dev Runtime (Классы)
Допустим у вас существует `JavaExec` таск для тестирования вашего приложения из среды разработки. Как же включить обработку BuildThing?

```kts
val runDev = tasks.create<JavaExec>("runDev") {
    dependsOn("classes")

    classpath = sourceSets.main.get()
        .runtimeClasspath
    mainClass.set("net.im51111n355.myproject.JavaEntryPoint")
}

// Как же включить обработку BuildThing?
```

Для этого добавлен метод `processClassesBeforeTask`, который создает и настраивает таск для обработки папки `classes` до того как будет выполнен ваш `JavaExec` таск.

К существующему коду из примера выше добавляем этот:

```kts
buildthing {
    // "DevRuntime" - Уникальная строка для названия таска ("processInPlace$name")
    // "runDev" - Таск ДО которого будет выполняться обработка
    processClassesBeforeTask("DevRuntime", runDev) {
        // Обязательно нужно указать чтобы обрабатывало ПОСЛЕ того как классы соберутся
        dependsOn("classes")
        // Дальше настройка как обычно
        config.values.put("build_time", System.currentTimeMillis())
    }
}
```

> ⚠️ По умолчанию "processClassesBeforeTask" создает таск с выключенной вырезалкой (Опция конфигурации `buildThingConfig.disableCutter = true`), но это можно выключить вручную.

### Обработка для Dev Runtime (Jar)
Некоторые плагины, например ForgeGradle, запускают тестовый инстанс игры из .jar билда, а не из папки classes. 

Для обхода этого существует метод `processJarBeforeTask` похожий на processClassesBeforeTask:

```kts
// Пример как настроить processJarBeforeTask для forge runClient

// "DevRuntime" - Уникальная строка для названия таска ("processInPlace$name")
// "runClient" - Таск ДО которого будет выполняться обработка .jar
processJarBeforeTask("DevRuntime", runClient) {
    // В отличии от processClassesBeforeTask - указывать зависимость от classes не нужно.
    
    // Дальше настройка как обычно
    config.values.put("build_time", System.currentTimeMillis())
}
```

> ⚠️ По умолчанию "processJarBeforeTask" создает таск с выключенной вырезалкой (Опция конфигурации `buildThingConfig.disableCutter = true`), но это можно выключить вручную.

### Инжекторы
Класс `Inject` с некоторыми полезными методами. Во время сборки все ваши вызовы в эти методы будут заменены на значения определённые во время сборки.

> После обработки BuildThing ВСЕ вызовы на методы `Inject` класса заменяются на значения определённые во время сборки.

> ⚠️ Для работы Inject класса в разработке (в Runtime) настройте "processClassesBeforeTask" на свой JavaExec класс который вы используете.

> ⚠️ В Inject методы можно передавать только константы.

### Поддержка Java Лямбд
Удаляет private synthetic реализации лямбд если после удаления на основе флагов не остается их вызовов.

Контроллируется настройкой `BuildThingConfig#deleteJavaStyleLambdas`, включено по умолчанию

### Поддержка Kotlin Лямбд
Удаляет методы которые считаются лямбдами в стиле Kotlin если после удаления на основе флагов не остается их вызовов.

Контроллируется настройкой `BuildThingConfig#deleteKotlinStyleLambdas`, включено по умолчанию

### Поддержка внутренних классов
Удаляет классы которые находятся внутри методов/других классов. 

Например, такие классы генерирует Kotlin для некоторых лямбд.

### Флаги
Альтернатива вариантам вырезания Client/Server/Любой другой ключ, с флагами можно реализовать включение/выключение функционала. 

Флаги устанавливаются в `BuildThingConfig.flags`, например:
```kts
// Добавляет в настройку билда флаг для включения клиента
config.flags.add("client")

// Добавляет в настройку билда флаг для включения сервера
config.flags.add("server")

// Добавляет в настройку билда флаг для включения отладки
config.flags.add("debugging")

// И так далее...
```

Дальше в Java/Kotlin коде вы можете отметить методы/поля/классы аннотацией @FlagCuttable. Указываемый аргумент - выражение, при значении `true` которого - метод/поле/класс кода нужно оставлять.

В последней версии BuildThing поддерживается указание не просто названия флага, но и именно выражений с `!`, `&&`, `||` и скобками.
```java
// Этот метод будет удален при сборке если нет флага "server" в конфиге при сборке.
// - Метод будет оставлен если есть флаг "server".
@FlagCuttable("server")
void someServerFunction() {
    // ...
}

// Этот метод будет оставлен если нет флага "server" 
@FlagCuttable("!server")
void someNotServerFunction() {
    // ...
}

// Этот метод будет оставлен если есть флаг "server" И/ИЛИ если есть флаг "keepSomeFunction"
@FlagCuttable("server || keepSomeFunction")
void someFunction() {
    // ...
}
```

Для некоторого функционала иногда очень полезно помечать не весь метод как серверный... 
В вырезалке от JustAGod это было решено через `Invoke.xxx` методы.

Т.К. в BuildThing может быть любое количество флагов - создание `Invoke` класса на пользователе библиотеки, но на самом деле это максимально просто...

Подобные классы и `invokeXXX` методы очень полезны чтобы правильно вырезать поля (Выполнять инициализацию поля в статичном/обычном конструкторе из invokeXXX метода) 

```java
// Вызовы методов с аннотацией @RemoveAtCallsite будут полностью удаляться (как и сам метод)
@RemoveAtCallsite
@FlagCuttable("server")
static void invokeServer(Runnable runnable) {
    runnable.run();
}

void mixedSideCode() {
    invokeServer(() -> {
        // Код для сервер стороны
        // Этот код как и сам факт существования вызова `invokeServer` будет вырезан.
        // ...
    });
    
    // Код для общей стороны
    // ...
}
```

> ⚠️ Теоретически можно использовать @RemoveAtCallsite не только для статичных методов с одним параметром лямбдой,
> но параметры переданные в такие функции могут быть не полностью вырезанны...
> 
> Как пример проблемы из-за этого - в байт-коде останется какая-то строка-константа которую вы передавали в @RemoveAtCallsite метод. 
> 
> В использовании как в примере - static + единственный параметр это Runnable (Может быть любой `FunctionalInterface`) - удаление лямбды гарантируется.

Дополнительно есть инжектор `Inject.flag(String)`, который как и аннотация - поддерживает выражения.

```java
public static final boolean IS_SERVER = Inject.flag("String");
```
> ⚠️ Т.К. обработка инжекторов применяется после компилятора - никогда не будет удалять содержимое if/else, независимо от состояния флагов.
>
> Например блок `if (Inject.flag("client")) { ... }` не удалится даже если флаг `client` выключен. 

### Валидация
Если вырезалка находит оставшиеся использования чего-то что будет удалено - будет написана ошибка.

### Рандомизация
Доступна замена значений во время сборки на случайно сгенерированные. Вызовы в методы `Inject.randInt(int, int)` или `randFloat`, `randLong`, `randDouble` будут заменяться на значения просчитанные во время сборки.

```java
// Заменит значение myField на случайное число во время сборки.
public final int myField = Inject.randInt(0, 100);

public void example() {
    // Это поле ТОЖЕ будет заменено во время сборки.
    int localVariable = Inject.randInt(0, 100);
}
```

### Значения
Доступна замена значений во время сборки на указанные в конфиге. Вызовы в методы `Inject.intValue(String)` или `floatValue`, `longValue`, `doubleValue`, `stringValue`, `booleanValue` будут заменяться на значения указанные в конфиге.

```kt
// Ставит значение "build_time" на текущее время 
config.values.put("build_time", System.currentTimeMillis())
```

```java
// Заменит значение BUILD_TIME во время сборки
public static final long BUILD_TIME = Inject.longValue("build_time");
```

### Списки классов
Есть аннотация `@ClassList` которая добавляет класс в список. Через вызов `Inject.classList(String)` вы можете получить список всех классов с указанным ключем.

Полезно для автоматической регистрации Minetweaker классов. 

```java
import java.util.List;

@ClassList("ClassListName")
class MyClass {
}

public void someMethod() {
    List<Class<?>> classes = Inject.classList("ClassListName");
    // ...
}
```

## Использование
Плагин доступен на Jitpack.io так что вам не нужно собирать его самому. 
Рекомендуется использовать последнюю (-SNAPSHOT) версию потому что вырезалка в разработке и могут быть исправления.

Добавляем строчки чтобы применить плагин:
```groovy
buildscript {
    repositories {
        maven {
            name = "Jitpack"
            url = "https://jitpack.io"
        }
        // ... Остальное тут
    }
    dependencies {
        classpath("com.github.51111N355:buildthing:-SNAPSHOT")
        // ... Остальное тут
    }
    // Чтобы при обновлении вырезалки Gradle не кешировал старую версию 24 часа
    // Не обязательно, но рекомендуется
    configurations.classpath.resolutionStrategy {
        cacheDynamicVersionsFor 180, 'seconds'
        cacheChangingModulesFor 180, 'seconds'
    }
}

// Ниже plugins { ... } блока если он у вас есть
apply plugin: "net.im51111n355.buildthing"
```

После применения плагина нужно создать таски для своих настроек сборки, например:

```kts
buildthing {
    buildProfile("Client") {
        config.flags.add("client")
    }

    buildProfile("Server") {
        config.flags.add("server")
    }
}
```

> ⚠️ Для корректной работы BuildThing важно использовать билды проектов которые обязательно прошли через BuildThing обработку.
> 
> Если вы, например, вырезаете только сервер (`buildClient` таск) - всеравно создайте `Server` таск и на сервере используйте обработанный BuildThing билд. 
> 
> Это важно для работы `Inject`, Удаления аннотаций, и возможно другого функционала. 

## Привязка к...


### ShadowJar
Чтобы применять адекватно после shadowJar - укажите в в buildthing второй аргумент - таск с которого будет обработка. 

Пример из реального проекта с разделением клиент/сервер:
```kts
buildthing {
    buildProfile("Client", shadowJar) {
        config.flags.add("client")
    }

    buildProfile("Server", shadowJar) {
        config.flags.add("server")
    }
}
```

### Forge Reobf 1.7.10
Применить Reobf Forge очень просто, добавляете записи как в примере ниже и билдить через `gradle build`
```groovy
buildthing {
    buildProfile("Client", shadowJar) {
        config.flags.add("client")

        // ЭТО включает forge reobf
        reobf.reobf(buildClient) { spec ->
            spec.classpath = sourceSets.main.compileClasspath
        }
    }

    buildProfile("Server", shadowJar) {
        config.flags.add("server")

        // ЭТО включает forge reobf
        reobf.reobf(buildServer) { spec ->
            spec.classpath = sourceSets.main.compileClasspath
        }
    }
}
```

### Forge runClient 1.7.10
Применять BT обработку во время runClient/runServer тасков очень просто.
```groovy
buildthing {
    processJarBeforeTask("DevRunClient", runClient/*, null - null нужен в Groovy DSL, но не нужен в Kotlin DSL*/) {
        config.values.put("some_value", 25)
    }
}
```

### MANIFEST.MF
Без дополнительной настройки MANIFEST.MF будет сбрасываться на тасках сборки BuildThing, но это легко исправить.

Пример из реального проекта с разделением клиент/сервер:
```groovy
buildthing {
    buildProfile("Client", shadowJar) {
        manifest = rootProject.tasks.jar.manifest
        config.flags.add("client")
    }

    buildProfile("Server", shadowJar) {
        manifest = rootProject.tasks.jar.manifest
        config.flags.add("server")
    }
}
```