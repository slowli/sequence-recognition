package ua.kiev.icyb.bio;

public class Sequence {
	
	public final byte[] observed;
	
	public final byte[] hidden;
	
	private final String id;
	
	public final int index;
	
	public final SequenceSet set;
	
	public Sequence(SequenceSet set, int index, String id, byte[] observed, byte[] hidden) {
		this.set = set;
		this.index = index;
		this.id = id;
		this.observed = observed;
		this.hidden = hidden;
	}
	
	public String id() {
		return (id == null) ? ("" + index) : id;
	}
	
	public int length() {
		return observed.length;
	}
}
