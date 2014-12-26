package ua.kiev.icyb.bio;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ua.kiev.icyb.bio.res.ResourceBundleUtils;

/**
 * Аннотация для методов, которые соответствуют исполняемым заданиям.
 * 
 * Задание соответствует отдельному запуску виртуальной машины Java.
 * Метод с аннотцией должен обладать сигнатурой
 * <pre>
 * public static void <i>&lt;произвольное имя метода&gt;</i>(String[] args)
 * </pre>
 * где <code>args</code> — массив аргументов, передаваемых заданию.
 * 
 * @see TaskRunner
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Task {
	/**
	 * Идентификатор задания.
	 * 
	 * @return
	 *    строка, использующаяся для идентификации задания
	 */
	String id();
	
	/**
	 * Имя файла ресурсов, содержащего описание задания и его аргументов.
	 * Описание задания должно содержаться в строке с идентификатором {@code "task." + id},
	 * описание аргументов — в строках вида {@code "task." + id + "." + i}, где <code>i</code>
	 * — номер аргумента. Например, заданию с идентификатором <code>dummy</code> могут соответствовать
	 * строки файла ресурсов
	 * <pre>
	 * task.dummy=Dummy task
	 * task.dummy.0=First dummy argument
	 * task.dummy.1=Second dummy argument
	 * </pre>
	 * 
	 * Файлы ресурсов загружатся с помощью класса {@link ResourceBundleUtils}.
	 * 
	 * @see ResourceBundleUtils#getBundle(String)
	 * 
	 * @return
	 *    имя файла ресурсов
	 */
	String res() default "";
}
