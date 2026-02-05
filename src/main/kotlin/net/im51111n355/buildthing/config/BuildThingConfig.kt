package net.im51111n355.buildthing.config

import java.io.Serializable

class BuildThingConfig : Serializable {
    // Флаги для сборки (вырезалка)
    val flags = mutableSetOf<String>()

    // Значения для сборки
    val values = mutableMapOf<String, Any>()

    // Полное отключение вырезалки. Не влияет на значения Inject.flag, но полностью пропускает этап вырезания @FlagCuttable.
    // Задумано для использования самой BuildThing чтобы пропускать вырезание в processXxxForExec, но можно включить и для своих тасков.
    var disableCutter = false

    // Удаление лямбда методов, которые в Java компилируются как private static synthetic, если этот метод вызывается только из метода который тоже будет удалён
    var deleteJavaStyleLambdas = true

    // В котлин - реализации лямбд генерируются как private static final, без synthetic на самой реализации
    // Эта настройка разрешает сносить private static final методы с "$lambda" в названии, только если они не вызываются в рамках класса.
    var deleteKotlinStyleLambdas = true
}