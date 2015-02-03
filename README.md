sequence-recognition
====================

Библиотека sequence-recognition предназначена для работы с алгоритмами распознавания
скрытых последовательностей в области биоинформатики (определение фрагментов 
генов - экзонов и интронов; определение вторичной структуры белка). 
Формулировки задач распознавания и другие сведения можно получить из 
документации (директория **docs/**), а также на сайте проекта

    http://sestudy.edu-ua.net/bio

Содержимое
--------------------

Дистрибутив библиотеки включает в себя следующие директории и файлы:

 * **build/** --- построенная версия библиотеки.
 * **docs/** --- документация к библиотеке.
 * **lib/** --- используемые зависимости (библиотека BioJava 1.8).
 * **bio.seq/** --- исходные файлы библиотеки.

В дистрибутив включены следующие JAR-архивы:

 * **bio.seq-<версия>-full.jar**  
     Архив с библиотекой, дополнением для обработки данных и включенными 
     зависимостями (BioJava Legacy). Может использоваться самостоятельно.
 * **bio.seq-<версия>.jar**  
     Библиотека БЕЗ дополнения для обработки данных. Может использоваться 
     самостоятельно.
 * **bio.seq-<версия>-javadoc.jar**  
     Архив с документацией.

Зависимости
--------------------

Требуется Java Runtime Environment версии не ниже 1.6; желательно иметь 
64-битную версию операционной системы и виртуальной машины Java и не менее 
4 Гб оперативной памяти. Для построения из исходников необходима библиотека 
[BioJava Legacy](http://biojava.org/) версии 1.8+.

Построение
--------------------

Для построения библиотеки из исходников можно использовать программу Apache Ant.
Конфигурация режимов построения находится в файле **build.xml**. Результаты
построения размещаются в папках **build/** и **doc/**. 

Основные режимы построения:

 * **compile** --- компилирует java-файлы библиотеки.
 * **jar** --- создает jar-файлы библиотеки.
 * **javadoc** --- создает документацию к библиотеке.
 * **build-aux** --- создает вспомогательные файлы (напр., образец файла конфигурации).
 * **build-all** --- вызывает все перечисленные выше режимы построения.
 * **clean-all** --- очищает выходную папку построения.

Для построения документации необходим набор инструментов Java Development Kit.

