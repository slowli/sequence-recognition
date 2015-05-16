package ua.kiev.icyb.bio.tools;

import java.io.IOException;

import ua.kiev.icyb.bio.Env;

/**
 * Задание, печатающее информацию о файле конфигурации. 
 */
public class PrintEnv {

	/**
	 * Печатает информацию о файле конфигурации.
	 * 
	 * <p><b>Аргументы задания:</b>
	 * <ol>
	 * <li>название файла конфигурации.
	 * </ol>
	 * 
	 * @param args
	 *    аргументы задания
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Env env = new Env(args[0]);
		System.out.println(env.repr());
	}

}
