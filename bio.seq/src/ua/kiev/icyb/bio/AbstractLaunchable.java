package ua.kiev.icyb.bio;

import ua.kiev.icyb.bio.res.Messages;

/**
 * Реализация интерфейса {@link Launchable} с использованием механизма сериализации
 * Java. Соответственно, все данные о ходе выполнения алгоритма должны содержаться
 * в его сериализуемых полях. Если среди сериализуемых полей есть итерабельные контейнеры
 * (например, <code>List</code>), необходимы меры предосторожности, чтобы во время сохранения
 * в них не добавлялись и не удалялись объекты.
 * 
 * <p><b>Пример.</b> Класс, выводящий все числа от единицы до <code>n</code>.
 * <pre>
 * public class NumberAlgorithm extends AbstractLaunchable {
 *     private int n;
 *     private int index;
 *     
 *     public NumberAlgorithm(int n) {
 *         this.n = n;
 *         this.index = 0;
 *     }
 *     
 *     protected void doRun() {
 *         for (int i = this.index; i < this.n; this.index = ++i) {
 *             System.out.println(i + 1);
 *         }
 *     } 
 * }
 * </pre>
 */
public abstract class AbstractLaunchable implements Launchable {

	private static final long serialVersionUID = 1L;
	
	private transient Env env;
	
	@Override
	public Env getEnv() {
		return env;
	}
	
	@Override
	public synchronized void run(Env env) {
		this.env = env;
		doRun();
	}
	
	/**
	 * Выполняет алгоритм.
	 */
	protected abstract void doRun();
	
	/**
	 * Сохраняет данные алгоритма.
	 */
	protected void save() {
		String dest = getEnv().saveProgress();
		if (dest != null) {
			env.debug(1, Messages.format("misc.save", dest));
		}
	}
}
