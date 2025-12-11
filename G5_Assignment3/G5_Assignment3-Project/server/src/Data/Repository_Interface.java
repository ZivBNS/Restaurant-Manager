package Data;


public interface Repository_Interface<T> {	
	public boolean set(T objToSet);
	public boolean update(T objToUpdate);
    public boolean deleteById(int confimrationCode);
	public T getById(int id);

}
