package Data;

public interface Repository_Interface<T> {
	public boolean set(T objToSet);
	public T getByCode(String confimrationCode);
	public boolean updateByCode(T objToUpdate);
    public boolean deleteByCode(String confimrationCode);
}
