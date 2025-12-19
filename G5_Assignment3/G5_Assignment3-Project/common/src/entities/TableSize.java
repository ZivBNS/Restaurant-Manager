package entities;

public enum TableSize {
	TWO(2),
	FOUR(4),
	SIX(6),
	EIGHT(8),
	TEN(10),
	TWELVE(12),;
	
	private final int seats;

	TableSize(int seats) {
		this.seats = seats;
	}
	
	public int getSeats() {
		return seats;
	}
	
	public static TableSize fromSeats(int seats) {
		for (TableSize ts : values()) {
			if (ts.seats == seats) {
				return ts;
			}
		}
		throw new IllegalArgumentException("Invalid table size: " + seats);
	}
}
