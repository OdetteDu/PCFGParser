package cs224n.assignment;

import cs224n.util.Pair;

import java.util.ArrayList;

public class Entry<T> {
	
	private ArrayList<Pair<T, Double>> list;
	
	public Entry()
	{
		list = new ArrayList<Pair<T, Double>>();
	}
	
	public void add(Pair<T, Double> p)
	{
		list.add(p);
	}
	
	public void add(T t, Double d)
	{
		list.add(new Pair<T, Double>(t, d));
	}
	
	public Pair<T, Double> get(int i)
	{
		return list.get(i);
	}
	
	@Override
	public String toString()
	{
		return list.toString();
	}
}
