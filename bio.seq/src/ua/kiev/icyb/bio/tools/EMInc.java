package ua.kiev.icyb.bio.tools;

import java.io.IOException;
import java.util.Arrays;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.SequenceSet;
import ua.kiev.icyb.bio.alg.mixture.IncrementalEMAlgorithm;
import ua.kiev.icyb.bio.alg.mixture.MarkovMixture;

/**
 * Команда, строящая смеси марковских распределений при помощи EM-алгоритма
 * с последовательным добавлением компонент.
 * 
 * @see IncrementalEMAlgorithm
 */
public class EMInc {

	/**
	 * Строит смеси марковских распределений при помощи EM-алгоритма
	 * с последовательным добавлением компонент.
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
		
		IncrementalEMAlgorithm alg = new IncrementalEMAlgorithm();
		alg.set = set;

		Configuration conf = new Configuration(Arrays.copyOfRange(args, 3, args.length));
		
		if (conf.containsKey("mixture")) {
			alg.mixture = env.load(conf.get("mixture"));
			alg.fitInitialMixture = false;
			conf.remove("mixture");
		} else {
			final int order = Integer.parseInt(conf.get("order"));
			alg.mixture = new MarkovMixture(1, order, set);
			alg.mixture.model(0).train(set);
		}
		
		conf.setFields(alg);
		
		try {
			env.run(alg, args[2]);
		} finally {
			env.finalize();
		}
	}

}
