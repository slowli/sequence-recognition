package ua.kiev.icyb.bio;


/**
 * Алгоритм, который может быть сохранен в файл с помощью 
 * метода {@link #save()} и затем восстановлен с помощью загрузки из этого файла.
 * При этом после загрузки алгоритм продолжает вычисления с того же (или прибизительно
 * того же) места, когда он был сохранен.
 */
public interface Launchable {
	
	/**
	 * Задает файл, в который прозиводится сохранение алгоритма.
	 * 
	 * @param filename
	 *    имя файла
	 */
	public void setSaveFile(String filename);
	
	/**
	 * Выполняет алгоритм. В процессе выполнения на определенных этапах
	 * следует сохранять прогресс с помощью метода {@link #save()}.
	 */
	public void run();
	
	/**
	 * Сохраняет текущие данные алгоритма в файл, предварительно установленный
	 * методом {@link #setSaveFile(String)}. Если имя файла не установлено, сохранения
	 * не производится.
	 */
	public void save();
}
