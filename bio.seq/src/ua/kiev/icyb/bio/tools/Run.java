package ua.kiev.icyb.bio.tools;

import java.io.IOException;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.Launchable;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Задание, запускающее сохраненный объект на выполнение.
 * 
 * @see Launchable
 */
public final class Run {

	/**
	 * Запускает сохраненный объект на выполнение.
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
		Launchable task = env.load(args[1]);
		
		env.debug(1, Messages.format("misc.file", args[1]));
		try {
			env.run(task, args[1]);
		} finally {
			env.finalize();
		}
	}

}
