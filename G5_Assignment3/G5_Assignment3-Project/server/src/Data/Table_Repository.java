package Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import entities.Restaurant;
import entities.Restaurant_Table;

public class Table_Repository implements Repository_Interface<Restaurant_Table>{
	private DB_Controller db = DB_Controller.getInstance();
    private static Table_Repository TableRepositoryInstance = new Table_Repository();

	private Table_Repository(){
	}

	public static Table_Repository getInstance() {
		return TableRepositoryInstance;
	}
	
	@Override
	public void init() {
		int maxTableSize=0;
		List<Restaurant_Table> tablesList = new ArrayList<>();
	    String sql = "SELECT ID, TableNumber, Size, IsActive FROM Tables";

	    try (Statement stmt = db.getConnection().createStatement();
	         ResultSet rs = stmt.executeQuery(sql)) {

	        while (rs.next()) {
	            int id = rs.getInt("ID");
	            int tableNumber = rs.getInt("TableNumber");
	            int size = rs.getInt("Size");
	            boolean isActive = rs.getBoolean("IsActive");
	            Restaurant_Table table = new Restaurant_Table(id, tableNumber, size, isActive);
	            if (table.getSize()>maxTableSize) maxTableSize=table.getSize();
	            tablesList.add(table);
	        }

	        Restaurant.getInstance().setTables(tablesList);
	        Restaurant.setBiggestTableSize(maxTableSize);
	        System.out.println("Successfully loaded " + tablesList.size() + " tables into Restaurant instance. updated max table size in restaurant");
	        System.out.println("max table size in restaurant: " + maxTableSize);
	        System.out.println(tablesList.toString());

	    } catch (SQLException e) {
	        System.err.println("Error loading tables from database: " + e.getMessage());
	        e.printStackTrace();
	    }		
	}
	
	
	@Override
	public boolean set(Restaurant_Table objToSet) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean update(Restaurant_Table objToUpdate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteById(int confimrationCode) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Restaurant_Table getById(int id) {
		// TODO Auto-generated method stub
		return null;
	}


}
