# BuildThing by 51111n355
[![](https://jitpack.io/v/51111N355/buildthing.svg)](https://jitpack.io/#51111N355/buildthing)
- Инструментация для разделения Java, Kotlin проектов на стороны (И не только)

## Возможности

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

Дальше в Java/Kotlin коде вы можете отметить методы/поля/классы аннотацией @FlagCuttable
```java
// Этот метод будет удален при сборке если нет флага "server" в конфиге при сборке.
@FlagCuttable("server")
void someServerFunction() {
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

Дополнительно есть аннотация `@InjectFlag` чтобы подставить значение `static final boolean` поля значением флага.

```java
@InjectFlag("server")
public static final boolean IS_SERVER = true;
```
> ⚠️ Т.К. обработка `@InjectFlag` применяется после сборки - Java компилятор может раньше чем надо удалить содержимое `if (IS_SERVER) { /* ЭТОГО */ }` блока если по умолчанию стоит значение `false`.
> 
> В интернете предлагается использовать `null!=null ? X : Y` чтобы избавиться от inline'инга компилятора.

> ⚠️ `@InjectFlag` не будет удалять содержимое `if (IS_SERVER) { /* ЭТОГО */ }` блока даже если стоит значение `false`.

### Валидация
Если вырезалка находит оставшиеся использования чего-то что будет удалено - будет написана ошибка.

### Рандомизация
Аннотациями `@InjectRandom.Int`, `@InjectRandom.Float`, `@InjectRandom.Long`, `@InjectRandom.Double` можно установить случайное значение `static final` полю на время компиляции.
Можно указывать минимальное-максимальное значение.

```java
@InjectRandom.Int(
    minInclusive=1,
    maxExclusive=101
)
public static final int someValue = 0;
```

### Значения
Аннотациями `@InjectValue.Int`, `@InjectValue.Float`, `@InjectValue.Long`, `@InjectValue.Double`, `@InjectValue.Boolean`, `@InjectValue.String` можно установить значение `static final` полю на значение указанное в конфиге.
```kt
// Ставит значение "build_time" на текущее время 
config.values.put("build_time", System.currentTimeMillis())
```

```java
@InjectValue.Long("build_time")
public static final long BUILD_TIME = 0L;
```

В резульате примера - BUILD_TIME во время сборки будет подменено на время сборки.

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
        cacheDynamicVersionsFor 0, 'seconds'
        cacheChangingModulesFor 0, 'seconds'
    }
}

// Ниже plugins { ... } блока если он у вас есть
apply plugin: "net.im51111n355.buildthing"
```

После применения плагина нужно создать таски для своих настроек сборки, например в KTS:

```kts
val plugin = plugins.findPlugin(BuildThingPlugin::class.java)!!

plugin.sideTask("Client") {
    config.flags.add("client")
}

plugin.sideTask("Server") {
    config.flags.add("server")
}
```

Или в groovy

```groovy
import net.im51111n355.buildthing.BuildThingPlugin
def plugin = plugins.findPlugin(BuildThingPlugin)

plugin.sideTask("Client") {
    it.config.flags.add("client")
}

plugin.sideTask("Server") {
    it.config.flags.add("server")
}
```


## Привязка к...


### ShadowJar
Чтобы применять адекватно после shadowJar - укажите в в sideTask второй аргумент - таск с которого будет обработка. 

Пример из реального проекта с разделением клиент/сервер:
```groovy
def buildthing = plugins.findPlugin(BuildThingPlugin)

// Второй аргумент - shadowJar. Обработка buildThing будет проходить после того как shadowJar запаковал зависимости.
buildthing.sideTask("Client", shadowJar) {
    it.config.flags.add("client")
}

// Второй аргумент - shadowJar. Обработка buildThing будет проходить после того как shadowJar запаковал зависимости.
buildthing.sideTask("Server", shadowJar) {
    it.config.flags.add("server")
}
```

### Forge Reobf 1.7.10
На самом деле применить Reobf Forge очень просто, добавляете записи как в примере ниже и билдить через `gradle build`
```groovy
reobf.reobf(buildClient) { spec ->
    spec.classpath = sourceSets.main.compileClasspath
}

reobf.reobf(buildServer) { spec ->
    spec.classpath = sourceSets.main.compileClasspath
}
```

### MANIFEST.MF
Без дополнительной настройки MANIFEST.MF будет сбрасываться на тасках сборки BuildThing, но это легко исправить.

Пример из реального проекта с разделением клиент/сервер:
```groovy
def buildthing = plugins.findPlugin(BuildThingPlugin)

buildthing.sideTask("Client") {
    // Будет браться манифест из того который вы могли настроить в jar { manifest { .. } } блоке (таска Jar) 
    it.manifest = rootProject.tasks.jar.manifest
    it.config.flags.add("client")
}

buildthing.sideTask("Server") {
    // Будет браться манифест из того который вы могли настроить в jar { manifest { .. } } блоке (таска Jar)
    it.manifest = rootProject.tasks.jar.manifest
    it.config.flags.add("server")
}
```