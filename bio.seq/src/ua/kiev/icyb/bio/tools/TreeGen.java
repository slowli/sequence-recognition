package ua.kiev.icyb.bio.tools;

import java.io.IOException;
import java.util.Arrays;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.tree.FragmentSet;
import ua.kiev.icyb.bio.alg.tree.RuleTreeGenerator;

/**
 * Задание, генерирующее дерево разбиения на области компетентности.
 * 
 * @see RuleTreeGenerator
 */
public final class TreeGen {

	/**
	 * Генерирует дерево разбиения на области компетентности.
	 * 
	 * <p><b>Аргументы задания:</b>
	 * <ol>
	 * <li>название файла конфигурации;
	 * <li>название выборки, на которой строится смесь;
	 * <li>шаблон для сохранения промежуточных результатов;
	 * <li>параметры алгоритма в формате "ключ — значение".
	 * </ol>
	 * 
	 * @param args
	 *    аргументы задания
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Env env = new Env(args[0]);
		SequenceSet set = env.loadSet(args[1]);
		
		RuleTreeGenerator alg = new RuleTreeGenerator();
		alg.set = set;
		alg.percentages = new double[] { 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7 };
		alg.baseSets = FragmentSet.getSequences(set.observedStates(), 1).subsets();
		
		Configuration conf = new Configuration(Arrays.copyOfRange(args, 3, args.length));
		conf.setFields(alg);
		
		try {
			env.run(alg, args[2]);
		} finally {
			env.finalize();
		}
	}

}
