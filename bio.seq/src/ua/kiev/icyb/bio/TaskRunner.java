package ua.kiev.icyb.bio;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;

import ua.kiev.icyb.bio.alg.AlgTasks;
import ua.kiev.icyb.bio.alg.mixture.EMTasks;
import ua.kiev.icyb.bio.alg.tree.TreeTasks;
import ua.kiev.icyb.bio.res.Messages;
import ua.kiev.icyb.bio.res.ResourceBundleUtils;

/**
 * Входная точка пакета для запуска {@linkplain Task заданий}.
 * 
 * Для добавления заданий из других пакетов следует определять подкласс:
 * <pre>
 * class SubTaskRunner extends TaskRunner {
 *
 *     protected void addTasks() {
 *         super.addTasks();
 *         // Здесь добавляются классы, содержащие задания
 *         addTaskHolder(NewTasks.class);
 *     }
 *
 *     public static void main(String[] args) throws IllegalAccessException, InvocationTargetException {
 *         execute(new SubTaskRunner(), args);
 *     }
 * }
 * </pre>
 */
public class TaskRunner {
	
	/**
	 * Используемый обработчик заданий.
	 */
	private static TaskRunner runner;

	/**
	 * Выводит список зарегистрированных задач.
	 * 
	 * @param args
	 *    (игнорируется)
	 */
	@Task(id="list", res=Messages.BUNDLE_NAME)
	public static void list(String[] args) {
		for (String taskId : runner.getTaskIds()) {
			System.out.format("%-20s %s\n",
					taskId, runner.getDescription(taskId));
		}
	}

	/**
	 * Выводит помощь по заданному заданию.
	 * 
	 * @param args
	 *    идентификатор задания
	 */
	@Task(id="help",res=Messages.BUNDLE_NAME)
	public static void help(String[] args) {
		Method task = runner.getTask(args[0]);
		if (task == null) {
			System.err.println(Messages.format("tasks.not_found", args[0]));
			return;
		}
	
		System.out.println(Messages.format("tasks.name", args[0]));
		System.out.println();
		System.out.println(Messages.format("tasks.description", runner.getDescription(args[0])));
		System.out.println();
		
		String[] argComments = runner.getArgComments(args[0]);
		System.out.println(Messages.getString("tasks.args")); //$NON-NLS-1$
		if (argComments.length == 0) {
			System.out.println(Messages.getString("tasks.no_args")); //$NON-NLS-1$
		} else {
			for (int i = 0; i < argComments.length; i++) {
				System.out.format("%d. %s\n", i + 1, argComments[i]);
			}
		}
	}

	/**
	 * Отображение, связывающее идентификаторы заданий с представляющими их методами.
	 */
	private final Map<String, Method> tasks = new TreeMap<String, Method>();
	
	/**
	 * Создает новый обработчик заданий. В процессе создания в обработчик добавляются
	 * задания с помощью метода {@link #addTasks()}.
	 */
	public TaskRunner() {
		addTaskHolder(TaskRunner.class);
		addTasks();
	}

	/**
	 * Возвращает описание задания.
	 * 
	 * @param taskId
	 *    идентификатор задания
	 * @return
	 *    описание задания
	 */
	public String getDescription(String taskId) {
		Method task = tasks.get(taskId);
		if (task == null) return "";
		
		Task taskAnnotation = task.getAnnotation(Task.class);
		ResourceBundle bundle = ResourceBundleUtils.getBundle(taskAnnotation.res());
		if (bundle != null) {
			return ResourceBundleUtils.getString(bundle, "task." + taskAnnotation.id());
		} else {
			return Messages.getString("tasks.no_descr");
		}
	}
	
	/**
	 * Возвращает описание аргументов задания.
	 * 
	 * @param taskId
	 *    индентификатор задания
	 * @return
	 *    массив из описаний аргументов
	 */
	public String[] getArgComments(String taskId) {
		Method task = tasks.get(taskId);
		if (task == null) return new String[0];
		
		Task taskAnnotation = task.getAnnotation(Task.class);
		ResourceBundle bundle = ResourceBundleUtils.getBundle(taskAnnotation.res());
		if (bundle != null) {
			final String prefix = "task." + taskAnnotation.id() + ".";
			List<String> argComments = new ArrayList<String>();
			for (int i = 0; bundle.containsKey(prefix + i); i++) {
				argComments.add(ResourceBundleUtils.getString(bundle, prefix + i));
			}
	
			return argComments.toArray(new String[0]);
		} else {
			return new String[0];
		}
	}
	
	/**
	 * Добавляет в обработчик задания из других классов.
	 */
	protected void addTasks() {
		addTaskHolder(CommonTasks.class);
		addTaskHolder(AlgTasks.class);
		addTaskHolder(EMTasks.class);
		addTaskHolder(TreeTasks.class);
	}
	
	/**
	 * Добавляет в обработчик задания из определенного класса.
	 * 
	 * @param cls
	 *    класс, содержащий методы с аннотацией {@link Task}
	 */
	protected void addTaskHolder(Class<?> cls) {
		for (Method method : cls.getDeclaredMethods()) {
			Task taskAnnotation = method.getAnnotation(Task.class);
			if (taskAnnotation != null) {
				tasks.put(taskAnnotation.id(), method);
			}
		}
	}
	
	/**
	 * Возвращает зарегистрированные в этом обработчике задания.
	 * 
	 * @return
	 *    множество идентификаторов зарегистрированных заданий
	 */
	public Set<String> getTaskIds() {
		return tasks.keySet();
	}
	
	/**
	 * Возвращает метод, соответствующий заданию.
	 * 
	 * @param id
	 *    идентификатор задания
	 * @return
	 *    метод, соответствующий заданию
	 */
	public Method getTask(String id) {
		return tasks.get(id);
	}
	
	/**
	 * Запускает задание на исполнение.
	 * 
	 * @param args
	 *    идентификатор задания, за которым следуют его аргументы; если идентификатор
	 *    не указан (то есть <code>args.length == 0</code>), выводится список
	 *    всех зарегистрированных заданий
	 * @throws IllegalArgumentException
	 *    если неверно задан идентификатор задания
	 * @throws InvocationTargetException
	 *    если метод, соответствующий заданию, не обладает корректной сигнатурой 
	 * @throws IllegalAccessException 
	 *    в случае неверной области видимости метода
	 */
	public static void main(String args[]) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		execute(new TaskRunner(), args);
	}
	
	/**
	 * Запускает задание на исполнение.
	 * 
	 * @param runner
	 *    обработчик заданий, который будет использоваться для поиска задания
	 * @param args
	 *    идентификатор задания, за которым следуют его аргументы; если идентификатор
	 *    не указан (то есть <code>args.length == 0</code>), выводится список
	 *    всех зарегистрированных заданий
	 * @throws IllegalArgumentException
	 *    если неверно задан идентификатор задания
	 * @throws InvocationTargetException
	 *    если метод, соответствующий заданию, не обладает корректной сигнатурой 
	 * @throws IllegalAccessException 
	 *    в случае неверной области видимости метода
	 */
	public static void execute(TaskRunner runner, String[] args) throws IllegalAccessException, InvocationTargetException {
		Env.initialize();
		
		if (args.length == 0) {
			args = new String[] { "list" };
		}
		String[] mainArgs = Arrays.copyOfRange(args, 1, args.length);
		
		TaskRunner.runner = runner;
		Method method = runner.getTask(args[0]);
		if (method == null) {
			throw new IllegalArgumentException(Messages.format("tasks.not_found", args[0])); //$NON-NLS-1$
		}
		method.invoke(null, (Object) mainArgs);
		
		Env.executor().shutdownNow();
	}
}
