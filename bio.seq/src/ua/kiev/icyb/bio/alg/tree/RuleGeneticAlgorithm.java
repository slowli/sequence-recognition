package ua.kiev.icyb.bio.alg.tree;

import java.util.HashSet;
import java.util.Set;

import ua.kiev.icyb.bio.Env;
import ua.kiev.icyb.bio.alg.GeneticAlgorithm;
import ua.kiev.icyb.bio.alg.Organism;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Генетический алгоритм для отбора предикатов для построения областей компетентности.
 * После формирования каждого поколения над ним выполняются следующие действия:
 * <ol>
 * <li>множества, содержащие более половины возмножных цепочек состояний, заменяются
 * на свои дополнения; 
 * <li>из популяции удаляются близкие множества цепочек состояний.
 * </ol>
 */
public class RuleGeneticAlgorithm extends GeneticAlgorithm {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Расстояние по Хэммингу между множествами, на основании которого
	 * отсеиваются близкие наборы множеств.
	 * 
	 * @see FragmentSet#trim(java.util.Collection, int)
	 */
	public int trimDistance = 2;

	@Override
	protected void onGenerationFormed(Set<Organism> population) {
		Set<FragmentSet> sets = new HashSet<FragmentSet>();
		for (Organism item : population) {
			sets.add((FragmentSet) item);
		}
		FragmentSet.trim(sets, trimDistance);
		population.clear();
		for (FragmentSet set : sets) {
			population.add((Organism) set);
		}
		
		Env.debug(1, Messages.format("gen.after_trim", population.size()));
	}
	
	@Override
	public String repr() {
		String repr = super.repr() + "\n";
		repr += Messages.format("gen.trim_dist", this.trimDistance);
		return repr;
	}
}
