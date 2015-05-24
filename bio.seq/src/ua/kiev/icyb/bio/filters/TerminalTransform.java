package ua.kiev.icyb.bio.filters;

import java.io.Serializable;
import java.util.Arrays;

import ua.kiev.icyb.bio.Sequence;
import ua.kiev.icyb.bio.StatesDescription;
import ua.kiev.icyb.bio.Transform;
import ua.kiev.icyb.bio.res.Messages;

/**
 * Преобразование, добавляющее отдельное наблюдаемое состояние {@code '$'} 
 * в конец всех последовательностей.
 */
public class TerminalTransform implements Transform, Serializable {

	private static final long serialVersionUID = 1L;
	

	private static String insertCompleteStates(String states, int insertions) {
		if (states == null) return null;
		
		String newStates = "";
		final int pieceLength = states.length() / insertions;
		for (int pos = 0; pos < states.length(); pos += pieceLength) {
			newStates += states.substring(pos, pos + pieceLength) + "$";
		}
		return newStates;
	}
	
	@Override
	public StatesDescription states(StatesDescription original) {
		return StatesDescription.create(
				original.observed() + "$",
				original.hidden(),
				insertCompleteStates(original.complete(), original.nHidden()));
	}

	@Override
	public Sequence sequence(Sequence original) {
		byte[] observed = new byte[original.length() + 1];
		System.arraycopy(original.observed, 0, observed, 0, original.length());
		observed[original.length()] = (byte) original.states().nObserved();
		
		byte[] hidden = new byte[original.length() + 1];
		System.arraycopy(original.hidden, 0, hidden, 0, original.length());
		hidden[original.length()] = 0;
		
		return new Sequence(original.id == null ? null : (original.id + "!tail"), 
				observed, hidden)
			.setStates(this.states(original.states()));
	}

	@Override
	public Sequence inverse(Sequence transformed) {
		return new Sequence(
				Arrays.copyOf(transformed.observed, transformed.length() - 1),
				Arrays.copyOf(transformed.hidden, transformed.length() - 1));
	}

	@Override
	public String repr() {
		return Messages.getString("transform.terminal");
	}
}
