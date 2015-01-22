package ua.kiev.icyb.bio;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

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
public abstract class AbstractLaunchable implements Launchable, Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Имя файла, в который производятся сохранения.
	 */
	private transient String saveFile;
	
	/**
	 * Определяет, был ли остановлен процесс выполнения алгоритма в результате действий
	 * пользователя (например, он нажал {@code ^C}) или из-за возникшей ошибки.
	 */
	protected transient boolean interruptedByUser;
	
	protected transient boolean interruptedByError;
	
	/**
	 * {@inheritDoc}
	 * <p>Эта реализация интерфейса дополнительно сохраняет данные алгоритма, если 
	 * пользователь прерывает работу виртуальной машины Java, например, нажатием клавиш {@code ^C}.
	 */
	public synchronized void run() {
		preRun();
		
		Thread shutdownThread = new Thread(new Runnable() {

			@Override
			public void run() {
				if (!interruptedByError) {
					interruptedByUser = true;
					save();
				}
			}
		});
		interruptedByUser = false;
		interruptedByError = false;
		Runtime.getRuntime().addShutdownHook(shutdownThread);
		
		try {
			doRun();
		} catch (RuntimeException e) {
			interruptedByError = true;
			throw e;
		} catch (Error e) {
			interruptedByError = true;
			throw e;
		}
		
		Runtime.getRuntime().removeShutdownHook(shutdownThread);
		postRun();
	}
	
	/**
	 * Выполняет алгоритм.
	 */
	protected abstract void doRun();
	
	/**
	 * Дополнительные действия до выполнения основных инструкций алгоритма, например,
	 * проверка его аргументов. 
	 */
	protected void preRun() {
	}
	
	/**
	 * Дополнительные действия после выполнения основных инструкций алгоритма.
	 */
	protected void postRun() {
	}
	
	public void setSaveFile(String filename) {
		this.saveFile = filename;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>Эта имплементация метода дополнительно пытается перед сохранением скопировать предыдущее
	 * сохранение в файл, имя которого получается добавлением к исходному имени 
	 * тильды <code>'~'</code> (например, {@code test.dbg~} для {@code test.dbg}).
	 */
	public void save() {
		if (saveFile != null) {
			File save = new File(saveFile), backup = new File(saveFile + "~");
			if (save.exists()) {
				backup.delete();
				save.renameTo(backup);
			}
			
			try {
				IOUtils.writeObject(saveFile, this);
			} catch (IOException e) {
				Env.error(0, Messages.format("misc.save_error", e));
				throw new RuntimeException(e);
			}
			onSave();
		}
	}
	
	protected void onSave() {
		Env.debug(1, Messages.format("misc.save", saveFile));
	}
}
