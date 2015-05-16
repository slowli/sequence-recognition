#!/bin/bash
#############################
# Environment variables used by command scripts.
# Переменные окружения, используемые командами.
#############################

# Root directory of the installation
# Корневая директория установки.
root=`dirname $0`/..

# Version of the library
# Версия библиотеки.
lib_version=0.10.0

# Determines the running environment for commands.
#   0 - use the stable build (JAR file)
#   1 - use the latest build in the project directory (bio.seq/)
# Определяет версию библиотеки, используемую для выполнения комманд
#   0 - использовать стабильную версию (JAR-файл)
#   1 - использовать рабочую версию из директории проекта (bio.seq/)
DEBUG=0

# Default options for Java VM
# Опции по умолчанию для виртуальной машины Java.
JAVAOPTS="-Xms512m -Xmx4g"

# Java classpath
# Путь к модулям и классам Java.
if [[ $DEBUG == "1" ]]; then
    CLASSPATH=$root/bio.seq/bin:$root/lib/bytecode-1.8.1.jar:$root/lib/core-1.8.1.jar:$CLASSPATH
else
    CLASSPATH=$root/bin/bio.seq-$lib_version-full.jar:$CLASSPATH
fi

ENV_FILE=./env.conf
if [[ ! -f $ENV_FILE ]]; then
	ENV_FILE=$root/bin/env.conf
fi

export CLASSPATH \
	JAVAOPTS \
	ENV_FILE
