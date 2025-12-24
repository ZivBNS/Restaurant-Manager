package Data;


public interface Repository_Interface<T> {	
	public boolean set(T objToSet);
	public boolean update(T objToUpdate);
    public boolean deleteById(int id);
	public T getById(int id);
	public void init();

}
