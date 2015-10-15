package cs224n.util;

public class Triple<F, S, T> {
	F first;
	S second;
	T third;

	public F getFirst() {
		return first;
	}

	public S getSecond() {
		return second;
	}

	public T getThird() {
		return third;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Triple)) return false;

		@SuppressWarnings("unchecked")
		final Triple triple = (Triple) o;

		if (first != null ? !first.equals(triple.first) : triple.first != null) return false;
		if (second != null ? !second.equals(triple.second) : triple.second != null) return false;
		if (third != null ? !third.equals(triple.third) : triple.third != null) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = (first != null ? first.hashCode() : 0);
		result = 29 * result + (second != null ? second.hashCode() : 0);
		result = 31 * result + (third != null ? third.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "(" + getFirst() + ", " + getSecond() + ", " + getThird() + ")";
	}

	public Triple(F first, S second, T third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
}
