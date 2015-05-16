package ua.kiev.icyb.bio.tools;

import java.io.IOException;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.Representable;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Задание, печатающее информацию о сохраненном объекте.
 */
public final class Print {

	/**
	 * Печатает информацию о сохраненном объекте.
	 * 
	 * <p><b>Аргументы задания:</b>
	 * <ol>
	 * <li>название файла конфигурации;
	 * <li>название файла со сохраненным объектом.
	 * </ol>
	 * 
	 * @param args
	 *    аргументы задания
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Env env = new Env(args[0]);
		Object obj = env.load(args[1]);
		
		env.debug(1, Messages.format("misc.file", args[1]));
		if (obj instanceof Representable) {
			System.out.println(((Representable) obj).repr());
		} else {
			System.out.println(obj);
		}
	}
}
